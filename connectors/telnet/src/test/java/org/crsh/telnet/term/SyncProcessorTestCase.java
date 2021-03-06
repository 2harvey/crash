/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.crsh.telnet.term;

import test.shell.sync.SyncShell;
import org.crsh.telnet.term.processor.Processor;

public class SyncProcessorTestCase extends AbstractProcessorTestCase {

  @Override
  protected SyncTerm createTerm() {
    return new SyncTerm();
  }

  @Override
  protected SyncShell createShell() {
    return new SyncShell();
  }

  @Override
  protected Processor createProcessor(SyncTerm term, SyncShell shell) {
    return new Processor(term, shell);
  }

  @Override
  protected int getBarrierSize() {
    return 1;
  }
}
