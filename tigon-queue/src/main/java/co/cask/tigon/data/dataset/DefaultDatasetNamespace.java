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

package co.cask.tigon.data.dataset;


import co.cask.tigon.conf.CConfiguration;
import co.cask.tigon.conf.Constants;
import co.cask.tigon.data.Namespace;

import javax.annotation.Nullable;

/**
 * Default dataset namespace; which namespace, determined by the configuration setting 
 * {@link co.cask.tigon.conf.Constants.Dataset#TABLE_PREFIX}.
 */
public class DefaultDatasetNamespace implements DatasetNamespace {
  private final String namespacePrefix;
  private final Namespace namespace;

  public DefaultDatasetNamespace(CConfiguration conf, Namespace namespace) {
    String tableNamespace = conf.get(Constants.Dataset.TABLE_PREFIX);
    this.namespacePrefix = tableNamespace + ".";
    this.namespace = namespace;
  }

  @Override
  public String namespace(String name) {
    return namespacePrefix + namespace.namespace(name);
  }

  @Override
  @Nullable
  public String fromNamespaced(String name) {
    if (!name.startsWith(namespacePrefix)) {
      return null;
    }
    // will return null if doesn't belong to namespace
    return namespace.fromNamespaced(name.substring(namespacePrefix.length()));
  }
}
