package ng.gdg.devfestsw.rxgithubsearch.rx;


import android.widget.EditText;

import rx.Observer;

import static android.R.attr.enabled;

public class RxEditText {

    public static Observer<Boolean> enabled(final EditText editText) {
        return new Observer<Boolean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Boolean enabled) {
                editText.setEnabled(enabled);
            }
        };
    }

    public static Observer<Boolean> disabled(final EditText editText) {
        return new Observer<Boolean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Boolean disabled) {
                editText.setEnabled(!disabled);
            }
        };
    }
}
