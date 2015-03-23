package org.ajbrown.rankerizer.handler

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.inject.Inject
import groovy.json.JsonOutput
import groovy.util.logging.Slf4j
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.ajbrown.rankerizer.util.PageLink
import ratpack.func.Action
import ratpack.groovy.Groovy
import ratpack.handling.Chain

import java.util.concurrent.TimeUnit

/**
 * Ratpack action handler which makes cached requests to the GitHub API, and ranks repositories.
 *
 */
@Slf4j
class GithubApiHandler implements Action<Chain> {

    @Inject
    HTTPBuilder httpBuilder

    /**
     * Cache of responses from GitHub for 15 minutes, ensuring we're a good citizen.
     */
    def githubResponses = CacheBuilder.newBuilder()
            .maximumSize(1024)
            .expireAfterWrite( 15, TimeUnit.MINUTES )
            .build(
            new CacheLoader<String, Map>() {
                public Map load(String key) {
                    executeRequest( key )
                }
            });

    @Override
    void execute(Chain chain) throws Exception {

        Groovy.chain(chain) {

            /**
             * Retrieve a ranked list of repositories for the specified organization.
             */
            get("repos/:id") {
                def orgId = pathTokens.get("id")?.toLowerCase()

                blocking {

                    rankRepositories( loadRepositories( orgId ) )

                 } then { repos ->

                    context.response.contentTypeIfNotSet( "application/json" )
                    context.render JsonOutput.toJson( repos )
                }
            }

            /**
             * Retrieve the most recent commits for the specified repository.
             */
            get("repos/:orgId/:repoId/commits") {
                def repoName = "${pathTokens.get("orgId")}/${pathTokens.get("repoId")}"
                def page = request.queryParams['page']?.toInteger()

                blocking {

                    loadCommits( repoName, page )

                } then { repos ->

                    context.response.contentTypeIfNotSet( "application/json" )
                    context.render JsonOutput.toJson( repos )
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
        recursiveRequest( "https://api.github.com/orgs/${organizationId}/repos" )
    }

    /**
     * Load the most recent commits for the specified repository.
     * @param repoName must be the full name of the repo.  Ex: Netflix/archaius
     * @return
     */
    def List<Map> loadCommits( String repoName, Integer page = 1 ) {
        //Don't be fooled by the name -- we aren't recursing, just keeping it DRY.
        recursiveRequest( "https://api.github.com/repos/${repoName}/commits?page=${page}", 1 )
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
     * Recursive method which will retrieve all results from a series of paginated URLs. Note that this will
     * make a new request for each rel="next" link provided in the response.  You should always provide a max depth,
     * otherwise
     *
     * @param url the URL to call
     * @param data existing data to be appended by each request
     * @param maxDepth the maximum number of recursive calls that will be made.  Must be no greater than 10. Specifying 1 means no recursion.
     * @return
     */
    protected recursiveRequest( String url, Integer maxDepth = 10 ) {
        def data = []

        if( maxDepth == 0 ) {
            return data
        }

        //defense - don't allow large max depths
        maxDepth = maxDepth <= 10 ? maxDepth : 10

        def resp = githubResponses.get( url )

        data += resp.data

        if( resp.next ) {
            data += recursiveRequest( resp.next?.link, --maxDepth )
        }

        data
    }

    /**
     * Execute an HTTP request to the specified URL.
     * @param url
     * @return
     */
    protected executeRequest( String url ) {
        def next = null
        def data = []

        httpBuilder.request( url, Method.GET, ContentType.JSON ) { req ->
            headers.'User-Agent' = 'org.ajbrown/Rankerizer/0.0.1'

            response.success = { resp, json ->
                next = PageLink.parseHeader( resp.getLastHeader( "Link" )?.value )?.find{ it.rel == "next" }
                data = json
            }

            response.failure = { resp ->

                log.warn( "Failure while calling github API: ${resp}" )
                throw new Exception( "Failure while calling gtihub API: ${resp.statusLine}" )

            }
        }

        [ next: next, data: data ]
    }
}
