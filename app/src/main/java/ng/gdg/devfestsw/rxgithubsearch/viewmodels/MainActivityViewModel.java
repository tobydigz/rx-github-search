package ng.gdg.devfestsw.rxgithubsearch.viewmodels;


import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import ng.gdg.devfestsw.rxgithubsearch.models.GithubRateLimitError;
import ng.gdg.devfestsw.rxgithubsearch.models.GithubRepository;
import ng.gdg.devfestsw.rxgithubsearch.services.GithubSearchHttpService;
import ng.gdg.devfestsw.rxgithubsearch.services.GithubSearchService;

import rx.functions.Action1;
import rx.subjects.BehaviorSubject;
import rx.subjects.PublishSubject;

public class MainActivityViewModel {
    private GithubSearchService service;

    public PublishSubject<String> searchQuery;
    public PublishSubject<Boolean> searching;
    public PublishSubject<Boolean> nextPageRequested;
    public BehaviorSubject<List<GithubRepository>> repositories;
    public PublishSubject<Boolean> repositoriesFetched;
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
                           public void call(List<GithubRepository> repos) {
                               repositories.onNext(repos);
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
        service.loadNextPage()
               .subscribe(
                       new Action1<List<GithubRepository>>() {
                           @Override
                           public void call(List<GithubRepository> repos) {
                               repositories.onNext(repos);
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
        searching = PublishSubject.create();
        repositories = BehaviorSubject.create((List<GithubRepository>) new ArrayList<GithubRepository>());
        repositoriesFetched = PublishSubject.create();
        rateLimitExceeded = PublishSubject.create();
    }
    
    private void setupSubscriptions() {
        searchQuery
                .subscribe(new Action1<String>() {
                    @Override
                    public void call(String query) {
                        search(query);
                    }
                });

        nextPageRequested
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean load) {
                        if (load != null && load) {
                            loadNextPage();
                        }
                    }
                });

        repositories
                .subscribe(new Action1<List<GithubRepository>>() {
                    @Override
                    public void call(List<GithubRepository> repositories) {
                        repositoriesFetched.onNext(true);
                    }
                });
    }
}
