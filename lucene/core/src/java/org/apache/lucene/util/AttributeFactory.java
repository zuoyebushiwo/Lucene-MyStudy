package org.apache.lucene.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * An AttributeFactory creates instances of {@link AttributeImpl}s.
 */
public abstract class AttributeFactory {

  /**
   * Returns an {@link AttributeImpl} for the supplied {@link Attribute} interface class.
   */
  public abstract AttributeImpl createAttributeInstance(Class<? extends Attribute> attClass);
  
  /**
   * Returns a correctly typed {@link MethodHandle} for the no-arg ctor of the given class.
   */
  static final MethodHandle findAttributeImplCtor(Class<? extends AttributeImpl> clazz) {
    try {
      return lookup.findConstructor(clazz, NO_ARG_CTOR).asType(NO_ARG_RETURNING_ATTRIBUTEIMPL);
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new IllegalArgumentException("Cannot lookup accessible no-arg constructor for: " + clazz.getName(), e);
    }
  }
  
  private static final MethodHandles.Lookup lookup = MethodHandles.publicLookup();
  private static final MethodType NO_ARG_CTOR = MethodType.methodType(void.class);
  private static final MethodType NO_ARG_RETURNING_ATTRIBUTEIMPL = MethodType.methodType(AttributeImpl.class);
  
  /**
   * This is the default factory that creates {@link AttributeImpl}s using the
   * class name of the supplied {@link Attribute} interface class by appending <code>Impl</code> to it.
   */
  public static final AttributeFactory DEFAULT_ATTRIBUTE_FACTORY = new DefaultAttributeFactory();
      
  private static final class DefaultAttributeFactory extends AttributeFactory {

    @Override
    public AttributeImpl createAttributeInstance(
        Class<? extends Attribute> attClass) {
      return null;
    }
    
  }
  
}
