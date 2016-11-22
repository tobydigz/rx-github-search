package ng.gdg.devfestsw.rxgithubsearch.rx;

import android.support.v7.widget.RecyclerView;
import rx.Observer;

import static android.R.attr.enabled;

public class RxRecyclerView {

    public static Observer<Boolean> enabled(final RecyclerView recyclerView) {
        return new Observer<Boolean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Boolean enabled) {
                recyclerView.setLayoutFrozen(!enabled);
            }
        };
    }

    public static Observer<Boolean> disabled(final RecyclerView recyclerView) {
        return new Observer<Boolean>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(Boolean disabled) {
                recyclerView.setLayoutFrozen(disabled);
            }
        };
    }
}
