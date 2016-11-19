package ng.gdg.devfestsw.rxgithubsearch.models;

import static android.R.id.message;

public class GithubError extends Exception {

    public GithubError(String message) {
        super(message);
    }

    @Override
    public String toString() {
        return "GithubError(" + message + ")";
    }
}
