package ng.gdg.devfestsw.rxgithubsearch.services;

import java.util.List;

import ng.gdg.devfestsw.rxgithubsearch.models.GithubRepository;
import rx.Observable;

public interface GithubSearchService {

    Observable<List<GithubRepository>> search(String query);

    Observable<List<GithubRepository>> loadNextPage();

    Observable<Boolean> cancel();
}