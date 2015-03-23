package org.ajbrown.rankerizer.handler

import com.google.common.cache.LoadingCache
import org.ajbrown.rankerizer.util.PageLink
import spock.lang.Specification

/**
 *
 */
class GithubApiHandlerSpec extends Specification {

    def handler = new GithubApiHandlerStub()

    def setup() {
        handler.githubResponses = Mock(LoadingCache)
    }

    def "repositories are ranked by number of watchers"() {

        def repos = [
                [ name: "My repo #1", watchers: 55 ],
                [ name: "My repo #2", watchers: 100 ],
                [ name: "My repo #3", watchers: 10 ],
                [ name: "My repo #4", watchers: 200 ],
                [ name: "My repo #5", watchers: 400, ],
                [ name: "My repo #6", watchers: 300 ],
        ]

        when:

        def result = handler.rankRepositories( repos )

        then:

        result.first().watchers == 400
        result.collect{ it.name } == [ "My repo #5", "My repo #6", "My repo #4", "My repo #2", "My repo #1", "My repo #3" ]
    }

    def "recursive requests will follow next link and append all response data"(){

        def nextLinks = [
                "http://www.example.com?page=2",
                "http://www.example.com?page=3"
        ]


        when:

        def resp = handler.recursiveRequest( "http://www.example.com" )

        then:

        1 * handler.githubResponses.get( "http://www.example.com" ) >> [ next : new PageLink( link: nextLinks[0] ), data: [1] ]
        1 * handler.githubResponses.get( nextLinks[0] ) >> [ next : new PageLink( link: nextLinks[1] ), data: [2] ]
        1 * handler.githubResponses.get( nextLinks[1] ) >> [ next : null, data: [3] ]

        resp == [1, 2, 3]
    }

    def "recursive request will not recurse more than maxDepth times"() {

        when:

        handler.recursiveRequest( "http://www.example.com", 10 )

        then:

        10 * handler.githubResponses.get( _ as String ) >> [ next: new PageLink( link: "http://www.example.com" ) ]

    }

    def "maxDepth for recursive requests can never be larger than 10"() {

        when:

        handler.recursiveRequest( "http://www.example.com", 20 )

        then:

        10 * handler.githubResponses.get( _ as String ) >> [ next: new PageLink( link: "http://www.example.com" ) ]

    }

}

/**
 * A testing stub for {@link GithubApiHandler} to expose protected methods.
 */
class GithubApiHandlerStub extends GithubApiHandler {

    public recursiveRequest( String url, Integer maxDepth = 10 ){
        super.recursiveRequest( url, maxDepth )
    }
}
