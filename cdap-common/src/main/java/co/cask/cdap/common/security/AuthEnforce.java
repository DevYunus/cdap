/*
 * Copyright © 2015 Cask Data, Inc.
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

package co.cask.cdap.common.security;

import co.cask.cdap.proto.security.Action;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a method that needs Authorization
 * <p>
 * {@link AuthEnforce#entity()}: Specifies the entity on which authorization will be enforced.
 * It can either be a variable name of the EntityId or an array of Strings from which the entity id can be constructed.
 * These variable names will be first looked up in the method parameter and if not found it will be looked up in the
 * class member variable.
 * <p>
 * {@link AuthEnforce#enforceOn()}: Class name of one of the CDAP entities on which enforcement will be done. If you
 * want to enforce on the parent of the entity specify that EntityId class here
 * <p>
 * {@link AuthEnforce#privileges()}: Array of Action to be checked during enforcement
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AuthEnforce {

  // Specifies the entity on which authorization will be enforced. It can either be a variable name of the EntityId or
  // an array of Strings from which the entity id can be constructed. These variable names will be first looked up in
  // the method parameter and if not found it will be looked up in the class member variable.
  String[] entity();

  // Class name of one of the CDAP entities on which enforcement will be done. If you want to enforce on the parent
  // of the entity specify that EntityId class here
  Class enforceOn();

  // Array of Action to be checked during enforcement
  Action[] privileges();
}