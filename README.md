GitHub Rankerizer
-----------------------------

GitHub Rankerizer is a small [Ratpack](http://www.ratpack.io/) and [AngularJS](https://angularjs.org/) application
which can list an organizations public Github repositories, ranked by audience size.

Audience is determined by the number of watchers.


### Running Rankerizer

This application can be run locally using the gradle wrapper.  **Java 8 is required** by Ratpack, and therefore by Rankerizer.

```
./gradlew run
```

Once complete, rankerizer will be running at `http://localhost:5050`


### Testing

Test for the backend are implemented as [Spock](https://code.google.com/p/spock/) specifications.  To run the test suite,
use the `test` gradle task:

```
./gradlew test
```