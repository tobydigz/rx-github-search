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
import com.jakewharton.rxbinding.view.RxView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.jakewharton.rxbinding.widget.TextViewAfterTextChangeEvent;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ng.gdg.devfestsw.rxgithubsearch.R;
import ng.gdg.devfestsw.rxgithubsearch.adapters.GithubRepositoriesAdapter;
import ng.gdg.devfestsw.rxgithubsearch.models.GithubRepository;
import ng.gdg.devfestsw.rxgithubsearch.viewmodels.MainActivityViewModel;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;

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
                .subscribe(viewModel.isSearching);

        RxTextView.afterTextChangeEvents(searchEditText)
                .debounce(THROTTLE_DELAY, TimeUnit.MILLISECONDS)
                .map(new Func1<TextViewAfterTextChangeEvent, String>() {
                    @Override
                    public String call(TextViewAfterTextChangeEvent event) {
                        return event.editable().toString();
                    }
                })
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String query) {
                        return query != null && !(query.isEmpty());
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

        viewModel.isSearching
                .subscribe(RxView.visibility(searchProgressBar));

        viewModel.isSearching
                .map(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean searching) {
                        return !searching;
                    }
                })
                .subscribe(RxView.visibility(repositoriesView));

        viewModel.isSearching
                .map(new Func1<Boolean, String>() {
                    @Override
                    public String call(Boolean searching) {
                        return searching ? getResources().getString(R.string.searching) : null;
                    }
                })
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s != null && !(s.trim().isEmpty());
                    }
                })
                .subscribe(RxTextView.text(repositoryCountTextView));

        viewModel.isLoadingNextPage
                .subscribe(RxView.visibility(loadNextPageProgressBar));

        viewModel.repositories
                .subscribe(new Action1<List<GithubRepository>>() {
                    @Override
                    public void call(List<GithubRepository> repositories) {
                        adapter.setRepositories(repositories);
                    }
                });

        viewModel.repositoriesCount
                .map(new Func1<Integer, String>() {
                    @Override
                    public String call(Integer count) {
                        return count == 0 ? getResources().getString(R.string.no_repository_found) : getResources().getString(R.string.repositories_found, count, count > 1 ? "ies" : "y");
                    }
                })
                .subscribe(RxTextView.text(repositoryCountTextView));

        viewModel.rateLimitExceeded
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(final Boolean rateLimitExceeded) {
                        rateLimitExceededSnackbar.setText(getResources().getString(R.string.rate_limit_exceeded, 60, "s"));
                        rateLimitExceededSnackbar.show();

                        searchEditText.setEnabled(false);
                        repositoriesView.setLayoutFrozen(true);

                        rateLimitExceededWaitSubscription = Observable.interval(1, TimeUnit.SECONDS)
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Action1<Long>() {
                                    @Override
                                    public void call(Long value) {
                                        long seconds = 59 - value;

                                        if (seconds > 0) {
                                            rateLimitExceededSnackbar.setText(getResources().getString(R.string.rate_limit_exceeded, seconds, seconds > 1 ? "s" : ""));
                                        } else {
                                            rateLimitExceededSnackbar.dismiss();

                                            searchEditText.setEnabled(true);
                                            repositoriesView.setLayoutFrozen(false);

                                            if (rateLimitExceededWaitSubscription != null && !(rateLimitExceededWaitSubscription.isUnsubscribed())) {
                                                rateLimitExceededWaitSubscription.unsubscribe();
                                            }
                                        }
                                    }
                                });
                        }
                    });
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

        adapter = new GithubRepositoriesAdapter();
        repositoriesView.setLayoutManager(new LinearLayoutManager(this));
        repositoriesView.setAdapter(adapter);
    }
}
