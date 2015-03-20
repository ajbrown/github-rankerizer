;
(function( angular ) {
    "use strict";

    angular.module( "rankerizer", [] )

        .controller( 'HomeController', [

            '$scope',
            '$http',

            function( $scope, $http ) {

                $scope.organization = null;
                $scope.repository = null;

                $scope.repositories = [];

                $scope.rankIt = function( orgName ) {
                    $scope.organization = orgName;
                    $scope.commits = [];
                    $scope.repositories = [];
                    $scope.repository = null;

                    $http.get( '/api/ranks/' + orgName )
                        .success( function( data ) {
                            $scope.repositories = data;
                        })
                        .error( function() {
                            alert( 'There was an error :-(' );
                        } )
                };

                $scope.selectRepo = function( repo ) {
                    $scope.repository = repo;
                    $scope.commits = null;

                    $http.get( repo.commits_url.replace( '{/sha}', '' ) )
                        .success( function( data ) {
                            $scope.commits = data;
                        })
                };
            }
        ]);


})( window.angular );