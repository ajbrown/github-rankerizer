package org.ajbrown.rankerizer.handler

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.inject.Inject
import com.google.inject.Key
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.ajbrown.rankerizer.util.PageLink
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain
import ratpack.http.client.HttpClient

import java.util.concurrent.TimeUnit

/**
 * Created by ajbrown on 3/21/15.
 */
class GithubApiHandler implements Action<Chain> {

    /**
     * Cache of repositories list so we play nicely with GitHub's API.
     */
    def repositories = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite( 30, TimeUnit.MINUTES )
            .build(
            new CacheLoader<String, List<Map>>() {
                public List<Map> load(String key) {
                    return loadRepositories( key )
                }
            });


    @Inject
    HTTPBuilder httpBuilder

    @Override
    void execute(Chain chain) throws Exception {

        Groovy.chain(chain) {

            /**
             * Retrieve a ranked list of repositories for the specified organization.
             */
            get("repos/:id") {
                def orgId = pathTokens.get("id")?.toLowerCase()

                blocking {

                    rankRepositories( repositories.get( orgId ) )

                 } then { repos ->

                    context.response.contentTypeIfNotSet( "application/json" )
                    context.render JsonOutput.toJson( repos )
                }
            }

            /**
             * Retreive the most recent commits for the specified repository.
             */
            get("repos/:orgId/:repoId/commits") {

                def orgId  = pathTokens.get("orgId")
                def repoId = pathTokens.get("repoId")
                def client = context.get(HttpClient)

                client.get( new URI("https://api.github.com/repos/${orgId}/${repoId}/commits"), { action ->
                    action.headers.add "User-Agent", "ratpack-http-client"
                }).then { response ->

                    context.response.contentTypeIfNotSet( "application/json" )
                    context.render response.body.text
                }
            }
        }
    }

    /**
     * Load a list of repositories by organization.
     * @param organizationId
     * @return
     */
    def List<Map> loadRepositories( String organizationId ) {
        paginatedRequest( "https://api.github.com/orgs/${organizationId}/repos" )
    }

    /**
     * Sort the repositories (by watchers, descending)
     * @param repositories
     * @return
     */
    def List<Map> rankRepositories( List repositories ) {
        repositories.sort{ -1 * it.watchers }
    }

    /**
     * Tail recursive method which will retrieve all results from a series of paginated URLs. Note that this will
     * recursively
     * @param url
     * @return
     */
    private paginatedRequest( String url, List data = [], Integer maxDepth = 10 ) {
        def next = null

        if( !url || maxDepth == 0 ) { return data }

        httpBuilder.request( url, Method.GET, ContentType.JSON ) { req ->

            headers.'User-Agent' = 'org.ajbrown/Rankerizer/0.0.1'

            response.success = { resp, json ->
                next = PageLink.parseHeader( resp.getLastHeader( "Link" )?.value )?.find{ it.rel == "next" }
                data += json
            }
        }

        paginatedRequest( next?.link, data, maxDepth-- )
    }


}
