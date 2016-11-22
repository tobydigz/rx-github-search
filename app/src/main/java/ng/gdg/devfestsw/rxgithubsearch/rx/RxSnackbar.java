package ng.gdg.devfestsw.rxgithubsearch.rx;


import android.support.design.widget.Snackbar;

import rx.Observer;

public class RxSnackbar {

    public static Observer<String> text(final Snackbar snackbar) {
        return new Observer<String>() {
            @Override
            public void onCompleted() { }

            @Override
            public void onError(Throwable e) { }

            @Override
            public void onNext(String text) {
                snackbar.setText(text);
            }
        };
    }

    public static Observer<Boolean> visible(final Snackbar snackbar) {
        return new Observer<Boolean>() {
            @Override
            public void onCompleted() { }

            @Override
            public void onError(Throwable e) { }

            @Override
            public void onNext(Boolean isVisible) {
                if (isVisible) {
                    snackbar.show();
                } else {
                    snackbar.dismiss();
                }
            }
        };
    }
}
