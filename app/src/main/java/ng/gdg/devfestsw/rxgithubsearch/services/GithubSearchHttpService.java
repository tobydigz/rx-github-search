package ng.gdg.devfestsw.rxgithubsearch.services;


import android.content.Context;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import ng.gdg.devfestsw.rxgithubsearch.models.GithubError;
import ng.gdg.devfestsw.rxgithubsearch.models.GithubRateLimitError;
import ng.gdg.devfestsw.rxgithubsearch.models.GithubRepository;
import rx.Observable;
import rx.Subscriber;

public class GithubSearchHttpService implements GithubSearchService {

    private static final String URL = "https://api.github.com/search/repositories";

    private RequestQueue requestQueue;
    private boolean cancelled;
    private String previousPageURL;
    private String nextPageURL;

    public GithubSearchHttpService(Context context) {
        requestQueue = Volley.newRequestQueue(context);
        cancelled = false;
    }

    public Observable<List<GithubRepository>> search(String query) {
        return loadPage(URL + "?q=" + query);
    }

    public Observable<List<GithubRepository>> loadNextPage() {
        if (nextPageURL != null && !(nextPageURL.isEmpty())) {
            return loadPage(nextPageURL);
        } else {
            return Observable.create(new Observable.OnSubscribe<List<GithubRepository>>() {
                @Override
                public void call(Subscriber<? super List<GithubRepository>> subscriber) {
                    subscriber.onError(new GithubError("no next page."));
                }
            });
        }
    }

    public Observable<Boolean> cancel() {
        requestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });

        cancelled = true;

        return Observable.create(new Observable.OnSubscribe<Boolean>() {
            @Override
            public void call(Subscriber<? super Boolean> subscriber) {
                subscriber.onNext(true);

                if (!(subscriber.isUnsubscribed())) {
                    subscriber.onCompleted();
                }
            }
        });
    }

    private Observable<List<GithubRepository>> loadPage(final String url) {
        return Observable.create(new Observable.OnSubscribe<List<GithubRepository>>() {
            @Override
            public void call(final Subscriber<? super List<GithubRepository>> subscriber) {
                GithubSearchHttpRequest request = new GithubSearchHttpRequest(Request.Method.GET, url,
                        new Response.Listener<GithubSearchHttpResponse>() {
                            @Override
                            public void onResponse(GithubSearchHttpResponse response) {
                                List<GithubRepository> repositories = handleSuccessfulResponse(response);
                                subscriber.onNext(repositories);

                                if (!(subscriber.isUnsubscribed())) {
                                    subscriber.onCompleted();
                                }
                            }
                        },
                        new Response.ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError error) {
                                Throwable t = handleErrorResponse(error);
                                subscriber.onError(t);
                            }
                        });

                requestQueue.add(request);
            }
        });
    }

    private List<GithubRepository> handleSuccessfulResponse(GithubSearchHttpResponse response) {
        this.previousPageURL = response.previousPageURL;
        this.nextPageURL = response.nextPageURL;

        return response.repositories;
    }

    private Throwable handleErrorResponse(VolleyError error) {
        if (error.networkResponse == null) {
            return new GithubError(error.getMessage());
        }

        if (error.networkResponse.statusCode == 403) {
            return new GithubRateLimitError();
        }

        try {
            JSONObject object = new JSONObject(new String(error.networkResponse.data, "UTF-8"));
            return new GithubError(object.getString("message"));
        } catch (Exception exception) {
            return exception;
        }
    }

    private class GithubSearchHttpRequest extends JsonRequest<GithubSearchHttpResponse> {

        GithubSearchHttpRequest(int method, String url, Response.Listener<GithubSearchHttpResponse> listener, Response.ErrorListener errorListener) {
            super(method, url, null, listener, errorListener);
        }

        @Override
        protected Response<GithubSearchHttpResponse> parseNetworkResponse(NetworkResponse resp) {
            if (resp == null) {
                return Response.error(new VolleyError("an unexpected error occured."));
            }

            try {
                GithubSearchHttpResponse response = new GithubSearchHttpResponse();
                response.repositories = parseJSONResponse(new JSONObject(new String(resp.data, "UTF-8")));

                if (resp.headers.containsKey("Link")) {
                    String url = getNextPageURL(resp.headers.get("Link"));

                    if (url != null && !(url.isEmpty())) {
                        response.nextPageURL = url;
                    }
                }

                return Response.success(response, HttpHeaderParser.parseCacheHeaders(resp));

            } catch (JSONException e) {
                return Response.error(new VolleyError(e.getMessage()));
            } catch (Exception e) {
                return Response.error(new VolleyError(resp));
            }
        }

        private List<GithubRepository> parseJSONResponse(JSONObject object) throws JSONException {
            List<GithubRepository> repositories = new ArrayList<>();

            JSONArray items = object.getJSONArray("items");
            int length = items.length();

            for (int i = 0; i < length; ++i) {
                JSONObject item = items.getJSONObject(i);
                repositories.add(new GithubRepository(item.getLong("id"), item.getString("name"), item.getString("html_url")));
            }

            return repositories;
        }

        private String getNextPageURL(String linkHeader) {
            String[] linkValue = linkHeader.split(",");

            for (String link: linkValue) {
                if (link.contains("next")) {
                    String[] parts = link.split(";");

                    if (parts.length > 0) {
                        String next = parts[0];

                        if (next != null && !(next.isEmpty())) {
                            return next.substring(1, next.length() - 1);
                        }
                    }
                }
            }

            return null;
        }
    }

    class GithubSearchHttpResponse {
        private String previousPageURL;
        private String nextPageURL;
        private List<GithubRepository> repositories;
    }
}
