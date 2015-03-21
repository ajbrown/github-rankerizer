package org.ajbrown.rankerizer.util

import spock.lang.Specification

/**
 * Created by ajbrown on 3/21/15.
 */
class PageLinksSpec extends Specification {


    def "parse a populated link header"() {
        def header = "<https://api.github.com/organizations/913567/repos?page=2>; rel=\"next\", <https://api.github.com/organizations/913567/repos?page=3>; rel=\"last\""

        when:

        def result = PageLink.parseHeader( header )

        then:

        result.size() == 2
        result[0].rel == "next"
        result[0].link == "https://api.github.com/organizations/913567/repos?page=2"

        result[1].rel == "last"
        result[1].link == "https://api.github.com/organizations/913567/repos?page=3"

    }

}
