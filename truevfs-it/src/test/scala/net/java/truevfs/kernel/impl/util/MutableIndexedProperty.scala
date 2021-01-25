/*
 * Copyright © 2005 - 2021 Schlichtherle IT Services.
 * All rights reserved. Use is subject to license terms.
 */
package net.java.truevfs.kernel.impl.util

trait MutableIndexedProperty[-A, B] extends (A => B) {

  def update(index: A, value: B): Unit
}
