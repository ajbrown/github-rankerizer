package org.ajbrown.rankerizer.util
/**
 * Represents a Link header.
 */
class PageLink {

    String rel
    String link

    /**
     * Parse the specified header into a list of PageLinks.
     *
     * @param header
     * @return
     */
    static List<PageLink> parseHeader( String header ) {

        if( !header ) return

        header.split(",")?.collect{
            def parts = it.split( ";" )?.collect{ it.trim() }

            new PageLink(
                    link: parts[0]?.substring(1, parts[0].length()-1 ),
                    rel:  parts[1]?.substring(5, parts[1].length()-1 )
            )
        }
    }
}
