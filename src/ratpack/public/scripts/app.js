;
(function( angular ) {
    "use strict";

    angular.module( "rankerizer", [ 'ngSanitize' ] )

        .controller( 'HomeController', [

            '$scope',
            '$http',

            function( $scope, $http ) {

                $scope.loadingRepositories = false;
                $scope.loadingCommits = false;
                $scope.organization = null;
                $scope.repository = null;

                $scope.repositories = [];

                $scope.rankIt = function( orgName ) {
                    $scope.organization = orgName;
                    $scope.commits = [];
                    $scope.repositories = [];
                    $scope.repository = null;
                    $scope.loadingRepositories = true;

                    $http.get( '/api/repos/' + orgName )
                        .success( function( data ) {
                            $scope.repositories = data;
                            $scope.loadingRepositories = false;
                        })
                        .error( function() {
                            alert( 'There was an error :-(.  Check the spelling of the organization.  If it\'s correct, We may have hit a rate limit' );
                        } )
                };

                $scope.selectRepo = function( orgName, repo ) {
                    $scope.repository = repo;
                    $scope.commits = null;
                    $scope.loadingCommits = true;
                    $scope.commitsPage = 1;

                    $http.get( '/api/repos/' + orgName + '/' + repo.name + '/commits' )
                        .success( function( data ) {

                            //extract only the data we care about
                            $scope.commits = data;
                            $scope.loadingCommits = false;
                        })
                };
            }
        ])


        .directive( 'commit', [ '$sce', function( $sce ) {

           return {
               restrict: 'E',
               link: function( scope, elem ) {

                   var breakAt   = scope.commit.commit.message.indexOf( "\n" );

                   if( breakAt > 0 ) {
                       scope.commit.subject = scope.commit.commit.message.substring(0, breakAt );
                       scope.commit.body    = scope.commit.commit.message.substring(breakAt );

                       var msgParts = scope.commit.body.trim().split( "\n" );
                       console.log( msgParts );
                       scope.commit.body = "<p>" + msgParts.join( "</p><p>" ) + "</p>";

                   } else {
                       scope.commit.subject = scope.commit.commit.message;
                       scope.commit.body = null;
                   }

               },
               scope: {
                 commit: "=commit"
               },
               templateUrl: 'commit-message.html'
           }
        }]);
    ;


})( window.angular );