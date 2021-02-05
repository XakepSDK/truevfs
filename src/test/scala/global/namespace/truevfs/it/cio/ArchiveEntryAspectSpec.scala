/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package global.namespace.truevfs.it.cio

import global.namespace.truevfs.commons.cio.Entry.Access._
import global.namespace.truevfs.commons.cio.Entry.PosixEntity._
import global.namespace.truevfs.commons.cio.Entry.Size._
import global.namespace.truevfs.commons.cio.Entry.Type._
import global.namespace.truevfs.commons.cio.Entry._
import global.namespace.truevfs.driver.mock.MockArchiveDriverEntry
import global.namespace.truevfs.kernel.api._
import org.scalatest.matchers.should.Matchers._
import org.scalatest.prop.TableDrivenPropertyChecks.{whenever, _}
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks.forAll

/**
  * @author Christian Schlichtherle
  */
class ArchiveEntryAspectSpec extends AnyWordSpec {

  private def forAllNameAndType(test: (FsArchiveEntry, ArchiveEntryAspect[_]) => Unit): Unit = {
    forAll { name: String =>
      whenever (null ne name) {
        forAll(Table("type", FILE, DIRECTORY)) { tµpe =>
          val e = new MockArchiveDriverEntry(name, tµpe)
          val a = ArchiveEntryAspect(e)
          test(e, a)
        }
      }
    }
  }

  "An entry aspect" should {
    "have the same name and type than its associated entry" in {
      forAllNameAndType { (e, a) =>
        a.name shouldBe e.getName
        a.tµpe shouldBe e.getType
      }
    }

    "properly decorate its associated entry" in {
      forAllNameAndType { (e, a) =>
        a.dataSize = UNKNOWN
        a.dataSize shouldBe UNKNOWN
        a.dataSize = 0
        a.dataSize shouldBe 0
        a.dataSize shouldBe e.getSize(DATA)

        a.storageSize = UNKNOWN
        a.storageSize shouldBe UNKNOWN
        a.storageSize = 0
        a.storageSize shouldBe 0
        a.storageSize shouldBe e.getSize(STORAGE)

        a.createTime = UNKNOWN
        a.createTime shouldBe UNKNOWN
        a.createTime = 0
        a.createTime shouldBe 0
        a.createTime shouldBe e.getTime(CREATE)

        a.readTime = UNKNOWN
        a.readTime shouldBe UNKNOWN
        a.readTime = 0
        a.readTime shouldBe 0
        a.readTime shouldBe e.getTime(READ)

        a.writeTime = UNKNOWN
        a.writeTime shouldBe UNKNOWN
        a.writeTime = 0
        a.writeTime shouldBe 0
        a.writeTime shouldBe e.getTime(WRITE)

        a.executeTime = UNKNOWN
        a.executeTime shouldBe UNKNOWN
        a.executeTime = 0
        a.executeTime shouldBe 0
        a.executeTime shouldBe e.getTime(EXECUTE)

        forAll(Table("PosixEntity", USER, GROUP, OTHER)) { entity =>
          a.createPermission(entity) = None // unknown
          a.createPermission(entity) shouldBe None
          a.createPermission(entity) = Option(false) // not permitted
          a.createPermission(entity) shouldBe Option(false)
          a.createPermission(entity) = Option(true) // permitted
          a.createPermission(entity) shouldBe Option(true)
          a.createPermission(entity) shouldBe Option(e.isPermitted(CREATE, entity).orElse(null))

          a.readPermission(entity) = None // unknown
          a.readPermission(entity) shouldBe None
          a.readPermission(entity) = Option(false) // not permitted
          a.readPermission(entity) shouldBe Option(false)
          a.readPermission(entity) = Option(true) // permitted
          a.readPermission(entity) shouldBe Option(true)
          a.readPermission(entity) shouldBe Option(e.isPermitted(READ, entity).orElse(null))

          a.writePermission(entity) = None // unknown
          a.writePermission(entity) shouldBe None
          a.writePermission(entity) = Option(false) // not permitted
          a.writePermission(entity) shouldBe Option(false)
          a.writePermission(entity) = Option(true) // permitted
          a.writePermission(entity) shouldBe Option(true)
          a.writePermission(entity) shouldBe Option(e.isPermitted(WRITE, entity).orElse(null))

          a.executePermission(entity) = None // unknown
          a.executePermission(entity) shouldBe None
          a.executePermission(entity) = Option(false) // not permitted
          a.executePermission(entity) shouldBe Option(false)
          a.executePermission(entity) = Option(true) // permitted
          a.executePermission(entity) shouldBe Option(true)
          a.executePermission(entity) shouldBe Option(e.isPermitted(EXECUTE, entity).orElse(null))

          a.deletePermission(entity) = None // unknown
          a.deletePermission(entity) shouldBe None
          a.deletePermission(entity) = Option(false) // not permitted
          a.deletePermission(entity) shouldBe Option(false)
          a.deletePermission(entity) = Option(true) // permitted
          a.deletePermission(entity) shouldBe Option(true)
          a.deletePermission(entity) shouldBe Option(e.isPermitted(DELETE, entity).orElse(null))
        }
      }
    }
  }
}
