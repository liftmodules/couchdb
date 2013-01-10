/*
 * Copyright 2010-2013 WorldWide Conferencing, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.liftmodules
package couchdb

import java.net.ConnectException

import dispatch.classic.{Http, StatusCode}

import org.specs2.mutable.Specification

import net.liftweb.common._
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import net.liftweb.record.field.{IntField, StringField}
import DocumentHelpers.stripIdAndRev


package couchtestrecords {
  class Person private () extends CouchRecord[Person] {
    def meta = Person

    object name extends StringField(this, 200)
    object age extends IntField(this)
  }

  object Person extends Person with CouchMetaRecord[Person]

  class Company private () extends CouchRecord[Company] {
    def meta = Company

    object name extends StringField(this, 200)
  }

  object Company extends Company with CouchMetaRecord[Company]
}

/**
 * Systems under specification for CouchRecord.
 */
object CouchRecordSpec extends Specification  {
  "CouchRecord Specification".title
  sequential

  import CouchDB.defaultDatabase
  import couchtestrecords._

  val design: JObject =
    ("language" -> "javascript") ~
    ("views" -> (("people_by_age" ->  ("map" -> "function(doc) { if (doc.type == 'Person') { emit(doc.age, doc); } }")) ~
                 ("oldest"        -> (("map" -> "function(doc) { if (doc.type == 'Person') { emit(doc.name, doc.age); } }") ~
                                      ("reduce" -> "function(keys, values) { return Math.max.apply(null, values); }")))))

  def setup = {
    val database = new Database("test")
    (try { Http(database delete) } catch { case StatusCode(_, _) => () }) must not(throwA[ConnectException]).orSkip
    Http(database create)
    Http(database.design("test") put design)
    defaultDatabase = database
  }

  def assertEqualPerson(a: Person, b: Person) = {
    a.name.value must_== b.name.value
    a.age.value must_== b.age.value
  }

  def assertEqualRows(foundRows: Seq[Person], expectedRows: Seq[Person]) = {
    foundRows.length must_== expectedRows.length
    for ((found, expected) <- foundRows.toList zip expectedRows.toList) {
      found.id.valueBox must_== expected.id.valueBox
      found.rev.valueBox must_== expected.rev.valueBox
      assertEqualPerson(found, expected)
    }
  }

  "A couch record" should {
    def testRec1: Person = Person.createRecord.name("Alice").age(25)
    val testDoc1: JObject = ("age" -> 25) ~ ("name" -> "Alice") ~ ("type" -> "Person")
    def testRec2: Person = Person.createRecord.name("Bob").age(30)
    val testDoc2: JObject = ("age" -> 30) ~ ("extra1" -> "value1") ~ ("extra2" -> "value2") ~ ("name" -> "Bob") ~ ("type" -> "Person")
    def testRec3: Company = Company.createRecord.name("Acme")

    "give emtpy box on get when nonexistant" in {
      setup must_== ()

      Person.fetch("testdoc").isDefined must_== false
    }

    "be insertable" in {
      setup

      val newRec = testRec1
      newRec save

      assertEqualPerson(newRec, testRec1)
      newRec.saved_? must_== true
      newRec.id.valueBox.isDefined must_== true
      newRec.rev.valueBox.isDefined must_== true

      val Full(foundRec) = Person.fetch(newRec.id.valueBox.openOrThrowException("This is a test"))
      assertEqualPerson(foundRec, testRec1)
      foundRec.id.valueBox must_== newRec.id.valueBox
      foundRec.rev.valueBox must_== newRec.rev.valueBox
    }

    "generate the right JSON" in {
      setup
      val newRec = testRec1
      newRec save

      val foundDoc = Http(defaultDatabase(newRec.id.valueBox.openOrThrowException("This is a test")) fetch)
      compact(render(stripIdAndRev(foundDoc))) must_== compact(render(testDoc1))
    }

    "be deletable" in {
      setup
      val newRec = testRec1
      newRec.save

      newRec.id.valueBox.isDefined must_== true
      val id = newRec.id.valueBox.openOrThrowException("This is a test")

      Person.fetch(id).isDefined must_== true
      newRec.delete_!.isDefined must_== true
      Person.fetch(id).isDefined must_== false
      newRec.delete_!.isDefined must_== false

      newRec.save
      Http(defaultDatabase(newRec.id.valueBox.openOrThrowException("This is a test")) @@ newRec.rev.valueBox.openOrThrowException("This is a test") delete)
      newRec.delete_!.isDefined must_== false
    }

    "be fetchable in bulk" in {
      setup
      val newRec1, newRec2, newRec3 = testRec1

      newRec1.save
      newRec2.save
      newRec3.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true
      newRec3.saved_? must_== true

      val expectedRows = newRec1::newRec3::Nil

      Person.fetchMany(newRec1.id.valueBox.openOrThrowException("This is a test"),
        newRec3.id.valueBox.openOrThrowException("This is a test")).map(_.toList) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }
    }

    "support queries" in {
      setup

      val newRec1, newRec3 = testRec1
      val newRec2 = testRec2
      newRec1.save
      newRec2.save
      newRec3.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true
      newRec3.saved_? must_== true

      val expectedRows = newRec2::Nil

      Person.queryView("test", "people_by_age", _.key(JInt(30))) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }
    }

    "support queries returning documents" in {
      setup

      val newRec1 = testRec1
      val newRec2 = testRec2
      newRec1.save
      newRec2.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true

      val expectedRows = newRec1::newRec2::Nil

      Person.queryViewDocs("test", "oldest", _.dontReduce) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }
    }

    "support queries returning documents for a non-reducing view" in {
      setup

      val newRec1 = testRec1
      val newRec2 = testRec2
      newRec1.save
      newRec2.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true

      val expectedRows = newRec1::newRec2::Nil

      Person.queryViewDocs("test", "people_by_age", identity) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }
    }

    "support multiple databases for fetching" in {
      setup
      val database2 = new Database("test2")
      (try { Http(database2 delete) } catch { case StatusCode(_, _) => () }) must not(throwA[ConnectException]).orSkip
      Http(database2 create)

      val newRec = testRec1
      newRec.database = database2
      newRec.save

      newRec.saved_? must_== true

      val foundRecBox: Box[Person] = newRec.id.valueBox.flatMap{ id =>  Person.fetchFrom(database2, id)}
      foundRecBox.isDefined must_== true
      val Full(foundRec) = foundRecBox
      assertEqualPerson(foundRec, testRec1)
      foundRec.id.valueBox must_== newRec.id.valueBox
      foundRec.rev.valueBox must_== newRec.rev.valueBox

      newRec.id.valueBox.flatMap(id =>  Person.fetch(id)).isDefined must_== false
    }

    "support multiple databases for fetching in bulk" in {
      setup
      val database2 = new Database("test2")
      (try { Http(database2 delete) } catch { case StatusCode(_, _) => () }) must not(throwA[ConnectException]).orSkip
      Http(database2 create)

      val newRec1, newRec2, newRec3 = testRec1
      newRec1.database = database2
      newRec2.database = database2
      newRec3.database = database2
      newRec1.save
      newRec2.save
      newRec3.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true
      newRec3.saved_? must_== true

      val expectedRows = newRec1::newRec3::Nil

      Person.fetchManyFrom(database2,
        newRec1.id.valueBox.openOrThrowException("This is a test"),
        newRec3.id.valueBox.openOrThrowException("This is a test")
      ).map(_.toList) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }

      Person.fetchMany(newRec1.id.valueBox.openOrThrowException("This is a test"),
        newRec3.id.valueBox.openOrThrowException("This is a test")) must beLike {
        case Full(seq) => seq.isEmpty must_== true
      }
    }

    "support multiple databases for queries" in {
      setup
      val database2 = new Database("test2")
      (try { Http(database2 delete) } catch { case StatusCode(_, _) => () }) must not(throwA[ConnectException]).orSkip
      Http(database2 create)
      Http(database2.design("test") put design)

      val newRec1, newRec3 = testRec1
      val newRec2 = testRec2
      newRec1.database = database2
      newRec2.database = database2
      newRec3.database = database2
      newRec1.save
      newRec2.save
      newRec3.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true
      newRec3.saved_? must_== true

      val expectedRows = newRec2::Nil

      Person.queryViewFrom(database2, "test", "people_by_age", _.key(JInt(30))) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }

      Person.queryView("test", "people_by_age", _.key(JInt(30))) must beLike { case Full(seq) => seq.isEmpty must_== true }
    }

    "support multiple databases for queries returning documents" in {
      setup
      val database2 = new Database("test2")
      (try { Http(database2 delete) } catch { case StatusCode(_, _) => () }) must not(throwA[ConnectException]).orSkip
      Http(database2 create)
      Http(database2.design("test") put design)

      val newRec1 = testRec1
      val newRec2 = testRec2
      newRec1.database = database2
      newRec2.database = database2
      newRec1.save
      newRec2.save

      newRec1.saved_? must_== true
      newRec2.saved_? must_== true

      val expectedRows = newRec1::newRec2::Nil

      Person.queryViewDocsFrom(database2, "test", "oldest", _.dontReduce) must beLike {
        case Full(foundRows) => assertEqualRows(foundRows, expectedRows); 1 must_== 1
      }

      Person.queryViewDocs("test", "oldest", _.dontReduce) must beLike { case Full(seq) => seq.isEmpty must_== true }
    }
  }
}
