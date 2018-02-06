package tech.jianyue.auth;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.alipay.sdk.app.PayTask;

import java.util.Map;

/**
 * 描述: 支付宝相关授权操作
 * 作者: WJ
 * 时间: 2018/1/19
 * 版本: 1.0
 */
public class AuthBuildForZFB extends Auth.Builder {
    private boolean isShowLoading = true;
    private String mOrderInfo;

    AuthBuildForZFB(Context context) {
        super(context);
    }

    /**
     * 获取 SDK 版本号
     */
    public static String getSDKVersion(AuthActivity activity) {
        PayTask payTask = new PayTask(activity);
        return payTask.getVersion();
    }

    @Override           // 初始化资源
    void init() {
    }

    @Override           // 清理资源
    void destroy() {
        super.destroy();
    }

    @Override
    public AuthBuildForZFB setAction(@Auth.ActionZFB int action) {
        mAction = action;
        return this;
    }

    public AuthBuildForZFB payOrderInfo(String orderInfo) {
        mOrderInfo = orderInfo;
        return this;
    }

    /**
     * 默认为显示
     */
    public AuthBuildForZFB payIsShowLoading(boolean isShow) {
        isShowLoading = isShow;
        return this;
    }

    @Override
    public void build(AuthCallback callback) {
        super.build(callback);
        switch (mAction) {
            case Auth.Pay:
                Intent intent = new Intent(mContext, AuthActivity.class);
                intent.putExtra("Sign", Sign);
                mContext.startActivity(intent);
                break;
            default:
                if (mAction != -1) {
                    mCallback.onFailed("支付宝暂未支持的 Action");
                }
                destroy();
        }
    }

    void pay(Activity activity) {
        if (TextUtils.isEmpty(mOrderInfo)) {
            mCallback.onFailed("必须添加 OrderInfo, 使用 payOrderInfo(info) ");
            activity.finish();
        } else {
            new AuthBuildForZFB.Pay(activity, mCallback, isShowLoading).execute(mOrderInfo);
        }
        destroy();
    }

    private static class Pay extends AsyncTask<String, Void, Map<String, String>> {
        private AuthCallback callback;                                      // 回调函数
        boolean isShowLoading;
        private Activity activity;

        Pay(Activity activity, AuthCallback callback, boolean isShow) {
            this.callback = callback;
            this.activity = activity;
            this.isShowLoading = isShow;
        }

        @Override
        protected Map<String, String> doInBackground(String... strings) {
            try {
                PayTask pay = new PayTask(activity);
                return pay.payV2(strings[0], isShowLoading);
            } catch (Exception e) {
                return null;
            }
        }

        @Override
        protected void onPostExecute(Map<String, String> resultMap) {
            super.onPostExecute(resultMap);
            if (resultMap != null) {
                String memo = null;
                String result = null;
                String resultStatus = null;

                for (String key : resultMap.keySet()) {
                    if (TextUtils.equals(key, "resultStatus")) {
                        resultStatus = resultMap.get(key);
                    } else if (TextUtils.equals(key, "result")) {
                        result = resultMap.get(key);
                    } else if (TextUtils.equals(key, "memo")) {
                        memo = resultMap.get(key);
                    }
                }
                // 对于支付结果，请商户依赖服务端的异步通知结果。同步通知结果，仅作为支付结束的通知。
                // 判断 resultStatus 为 9000 则代表支付成功
                if (TextUtils.equals(resultStatus, "9000")) {               // 该笔订单是否真实支付成功，需要依赖服务端的异步通知
                    callback.onSuccessForPay(resultMap.toString());
                } else {                                                        // 判断resultStatus 为非“9000”则代表可能支付失败, 该笔订单真实的支付结果，需要依赖服务端的异步通知
                    if (TextUtils.equals(resultStatus, "8000")) {           // “8000”代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                        callback.onSuccessForPay(resultMap.toString());         // 默认为支付成功
                    } else {                                                    // 其他值就可以判断为支付失败，包括用户主动取消支付，或者系统返回的错误
                        callback.onFailed("支付宝支付失败");
                    }
                }
            } else {
                callback.onFailed("支付宝支付失败");
            }
            activity.finish();
            callback = null;
            activity = null;
        }
    }
}