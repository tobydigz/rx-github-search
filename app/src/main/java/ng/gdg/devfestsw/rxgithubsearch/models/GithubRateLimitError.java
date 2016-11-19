package ng.gdg.devfestsw.rxgithubsearch.models;


public class GithubRateLimitError extends Exception {

    public GithubRateLimitError() {
        super("rate limit exceeded.");
    }

    @Override
    public String toString() {
        return "rate limit exceeded.";
    }
}
