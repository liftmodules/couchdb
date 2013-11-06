lift-couchdb
============

[Lift Record] implementation providing [CouchDB] persistence for Lift Web Framework applications.

To include this module in your Lift project, update your `libraryDependencies` in `build.sbt` to include:

*Lift 2.6.x*:

    "net.liftmodules" %% "lift-couch_2.6" % "1.2"

*Lift 2.5.x*:

    "net.liftmodules" %% "lift-couch_2.5" % "1.2"

*Lift 3.0.x*:

    "net.liftmodules" %% "lift-couch_3.0" % "1.2-SNAPSHOT"


Documentation
-------------

* [Lift in Action](http://www.manning.com/perrett/), chapter 11, includes a short example of using CouchDB.

* [CouchDB: The Definitive Guide](http://guide.couchdb.org/index.html) for information about CouchDB itself.

**Note:** The module package changed from `net.liftweb.couchdb` to `net.liftmodules.couchdb` in December 2012.
Please consider this when referencing documentation written before that date.

Notes for module developers
---------------------------

Push to master triggers the [Jenkins build](https://liftmodules.ci.cloudbees.com/job/couchdb/).

`+test` fails with JRebel enabled.

[Lift Record]: https://www.assembla.com/spaces/liftweb/wiki/Record
[CouchDB]: http://couchdb.apache.org/
