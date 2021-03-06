/*
 * Copyright © 2014 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.tigon.data.hbase;

import co.cask.tigon.data.util.hbase.HBaseVersionSpecificFactory;

/**
 * Factory class to provide instances of the correct {@link HBaseTestBase} implementation, dependent on the version
 * of HBase that is being used.
 */
public class HBaseTestFactory extends HBaseVersionSpecificFactory<HBaseTestBase> {
  @Override
  protected String getHBase94Classname() {
    return "co.cask.tigon.data.hbase.HBase94Test";
  }

  @Override
  protected String getHBase96Classname() {
    return "co.cask.tigon.data.hbase.HBase96Test";
  }
}
