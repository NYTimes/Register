package com.nytimes.android.external.playbillingtester;


import com.nytimes.android.external.playbillingtester.model.Repository;

import io.reactivex.Observable;
import retrofit2.http.GET;

public interface GithubApi {
    @GET(BuildConfig.GITHUB_ENDPOINT)
    Observable<Repository> getPlayBillingRepository();
}