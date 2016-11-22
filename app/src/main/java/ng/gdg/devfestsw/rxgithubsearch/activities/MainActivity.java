package ng.gdg.devfestsw.rxgithubsearch.activities;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.jakewharton.rxbinding.support.v7.widget.RecyclerViewScrollEvent;
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerView;
import com.jakewharton.rxbinding.support.v7.widget.RxRecyclerViewAdapter;
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ng.gdg.devfestsw.rxgithubsearch.R;
import ng.gdg.devfestsw.rxgithubsearch.adapters.GithubRepositoriesAdapter;
import ng.gdg.devfestsw.rxgithubsearch.models.GithubRepository;
import ng.gdg.devfestsw.rxgithubsearch.rx.RxEditText;
import ng.gdg.devfestsw.rxgithubsearch.rx.RxSnackbar;
import ng.gdg.devfestsw.rxgithubsearch.viewmodels.MainActivityViewModel;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;

public class MainActivity extends AppCompatActivity {

    private final static int THROTTLE_DELAY = 600;

    MainActivityViewModel viewModel;

    private EditText searchEditText;
    private TextView repositoryCountTextView;
    private RecyclerView repositoriesView;
    private View searchProgressBar;
    private View loadNextPageProgressBar;
    private Snackbar rateLimitExceededSnackbar;

    private GithubRepositoriesAdapter adapter;

    private Subscription rateLimitExceededWaitSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupViewModel();
        setupViews();
        setupSubscriptions();
    }
    
    private void setupSubscriptions() {
        RxTextView.afterTextChangeEvents(searchEditText)
                .map(new Func1<TextViewAfterTextChangeEvent, Boolean>() {
                    @Override
                    public Boolean call(TextViewAfterTextChangeEvent event) {
                        return true;
                    }
                })
                .subscribe(viewModel.searching);

        RxTextView.afterTextChangeEvents(searchEditText)
                .debounce(THROTTLE_DELAY, TimeUnit.MILLISECONDS)
                .map(new Func1<TextViewAfterTextChangeEvent, String>() {
                    @Override
                    public String call(TextViewAfterTextChangeEvent event) {
                        return event.editable().toString();
                    }
                })
                .subscribe(viewModel.searchQuery);

        RxRecyclerView.scrollEvents(repositoriesView)
                .map(new Func1<RecyclerViewScrollEvent, Boolean>() {
                    @Override
                    public Boolean call(RecyclerViewScrollEvent event) {
                        LinearLayoutManager layoutManager = (LinearLayoutManager) repositoriesView.getLayoutManager();
                        int lastItemPosition = layoutManager.findLastCompletelyVisibleItemPosition();

                        return lastItemPosition != RecyclerView.NO_POSITION && lastItemPosition == adapter.getItemCount() - 1;
                    }
                })
                .subscribe(viewModel.nextPageRequested);

        viewModel.searching
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxView.visibility(searchProgressBar));

        viewModel.searching
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean searching) {
                        if (searching) {
                            adapter.clearRepositories();
                        }
                    }
                });

        viewModel.repositoriesFetched
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxView.visibility(repositoriesView));

        viewModel.repositoriesFetched
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean fetched) {
                        return !fetched;
                    }
                })
                .subscribe(RxView.visibility(searchProgressBar));

        viewModel.repositoriesFetched
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean fetched) {
                        return !fetched;
                    }
                })
                .subscribe(RxView.visibility(loadNextPageProgressBar));

        viewModel.nextPageRequested
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxView.visibility(loadNextPageProgressBar));

        viewModel.repositories
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    new Action1<List<GithubRepository>>() {
                        @Override
                        public void call(List<GithubRepository> repositories) {
                            adapter.addRepositories(repositories);
                        }
                    }
                );

        viewModel.searching
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Func1<Boolean, String>() {
                    @Override
                    public String call(Boolean searching) {
                        return getResources().getString(R.string.searching);
                    }
                })
                .subscribe(RxTextView.text(repositoryCountTextView));

        RxRecyclerViewAdapter.dataChanges(adapter)
            .map(new Func1<GithubRepositoriesAdapter, String>() {
                @Override
                public String call(GithubRepositoriesAdapter adapter) {
                    int count = adapter.getItemCount();

                    return count == 0 ? getResources().getString(R.string.no_repository_found)
                            : getResources().getString(R.string.repositories_found, count, count > 1 ? "ies" : "y");
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxTextView.text(repositoryCountTextView));

        viewModel.rateLimitExceeded
                .subscribe(RxSnackbar.visible(rateLimitExceededSnackbar));

        viewModel.rateLimitExceeded
                .subscribe(RxEditText.disabled(searchEditText));

        viewModel.rateLimitExceeded
                .subscribe(ng.gdg.devfestsw.rxgithubsearch.rx.RxRecyclerView.disabled(repositoriesView));

        viewModel.rateLimitExceeded
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean exceeded) {
                        return exceeded;
                    }
                })
                .flatMap(new Func1<Boolean, Observable<Long>>() {
                    @Override
                    public Observable<Long> call(Boolean exceeded) {
                        return Observable.zip(
                                Observable.range(0, 60),
                                Observable.interval(1, TimeUnit.SECONDS),
                                new Func2<Integer, Long, Long>() {
                                    @Override
                                    public Long call(Integer count, Long seconds) {
                                        return 59L - count;
                                    }
                                }
                        )
                        .doOnNext(new Action1<Long>() {
                            @Override
                            public void call(Long seconds) {
                                if (seconds == 0) {
                                    viewModel.rateLimitExceeded.onNext(false);
                                }
                            }
                        });
                    }
                })
                .map(new Func1<Long, String>() {
                    @Override
                    public String call(Long seconds) {
                        return getResources().getString(R.string.rate_limit_exceeded, seconds, seconds > 1 ? "s" : "");
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(RxSnackbar.text(rateLimitExceededSnackbar));
    }

    private void setupViewModel() {
        viewModel = new MainActivityViewModel(this);
    }

    private void setupViews() {
        searchEditText = (EditText) findViewById(R.id.search_text_field);
        repositoryCountTextView = (TextView) findViewById(R.id.repository_count_text_view);
        repositoriesView = (RecyclerView) findViewById(R.id.repositories_view);
        searchProgressBar = findViewById(R.id.search_progress_bar);
        loadNextPageProgressBar = findViewById(R.id.load_next_page_progress_bar);
        rateLimitExceededSnackbar = Snackbar.make(findViewById(R.id.activity_main), "", Snackbar.LENGTH_INDEFINITE);
        rateLimitExceededSnackbar.setText(getResources().getString(R.string.rate_limit_exceeded, 60, "s"));

        adapter = new GithubRepositoriesAdapter();
        repositoriesView.setLayoutManager(new LinearLayoutManager(this));
        repositoriesView.setAdapter(adapter);
    }
}
