package ng.gdg.devfestsw.rxgithubsearch.viewmodels;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ng.gdg.devfestsw.rxgithubsearch.models.GithubRateLimitError;
import ng.gdg.devfestsw.rxgithubsearch.models.GithubRepository;
import ng.gdg.devfestsw.rxgithubsearch.services.GithubSearchHttpService;
import ng.gdg.devfestsw.rxgithubsearch.services.GithubSearchService;

import rx.functions.Action1;
import rx.functions.Func1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class MainActivityViewModel {
    private GithubSearchService service;

    public PublishSubject<String> searchQuery;
    public PublishSubject<Boolean> nextPageRequested;
    public BehaviorSubject<List<GithubRepository>> repositories;
    public PublishSubject<Integer> repositoriesCount;
    public PublishSubject<Boolean> isSearching;
    public PublishSubject<Boolean> isLoadingNextPage;
    public PublishSubject<Boolean> rateLimitExceeded;

    public MainActivityViewModel(Context context) {
        setupGithubSearchService(context);
        setupObservers();
        setupSubscriptions();
    }

    private void search(String query) {
        service.search(query)
               .subscribe(
                       new Action1<List<GithubRepository>>() {
                           @Override
                           public void call(List<GithubRepository> repositories) {
                               MainActivityViewModel.this.repositories.onNext(repositories);
                           }
                       },
                       new Action1<Throwable>() {
                           @Override
                           public void call(Throwable throwable) {
                               if (throwable instanceof GithubRateLimitError) {
                                   rateLimitExceeded.onNext(true);
                               }
                           }
                       }
               );
    }

    private void loadNextPage() {
        isLoadingNextPage.onNext(true);

        service.loadNextPage()
               .subscribe(
                       new Action1<List<GithubRepository>>() {
                           @Override
                           public void call(List<GithubRepository> nextRepositories) {
                               List<GithubRepository> repositories = MainActivityViewModel.this.repositories.getValue();
                               if (repositories.addAll(nextRepositories)) {
                                   MainActivityViewModel.this.repositories.onNext(repositories);
                               }
                           }
                       },
                       new Action1<Throwable>() {
                           @Override
                           public void call(Throwable throwable) {
                               if (throwable instanceof GithubRateLimitError) {
                                   rateLimitExceeded.onNext(true);
                               }
                           }
                       }
               );
    }

    private void setupGithubSearchService(Context context) {
        service = new GithubSearchHttpService(context);
    }

    private void setupObservers() {
        searchQuery = PublishSubject.create();
        nextPageRequested = PublishSubject.create();
        repositories = BehaviorSubject.create((List<GithubRepository>) new ArrayList<GithubRepository>());
        repositoriesCount = PublishSubject.create();
        isSearching = PublishSubject.create();
        isLoadingNextPage = PublishSubject.create();
        rateLimitExceeded = PublishSubject.create();
    }

    private void setupSubscriptions() {
        searchQuery
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String query) {
                        MainActivityViewModel.this.search(query);
                    }
                });

        nextPageRequested
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean load) {
                        if (load != null && load) {
                            MainActivityViewModel.this.loadNextPage();
                        }
                    }
                });

        repositories
                .map(new Func1<List<GithubRepository>, Integer>() {
                    @Override
                    public Integer call(List<GithubRepository> repositories) {
                        return repositories.size();
                    }
                })
                .subscribe(repositoriesCount);

        repositories
                .subscribe(new Action1<List<GithubRepository>>() {
                    @Override
                    public void call(List<GithubRepository> repositories) {
                        isSearching.onNext(false);
                        isLoadingNextPage.onNext(false);
                    }
                });
    }
}
