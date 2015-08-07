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
    private final ClassValue<MethodHandle> constructors = new ClassValue<MethodHandle>() {
      @Override
      protected MethodHandle computeValue(Class<?> attClass) {
        return findAttributeImplCtor(findImplClass(attClass.asSubclass(Attribute.class)));
      }
    };
    
    DefaultAttributeFactory() {}
    
    @Override
    public AttributeImpl createAttributeInstance(
        Class<? extends Attribute> attClass) {
      try {
        return (AttributeImpl) constructors.get(attClass).invokeExact();
      } catch (Throwable t) {
        rethrow(t);
        throw new AssertionError();
      }
    }
    
    private Class<? extends AttributeImpl> findImplClass(Class<? extends Attribute> attClass) {
      try {
        return Class.forName(attClass.getName() + "Impl", true, attClass.getClassLoader()).asSubclass(AttributeImpl.class);
      } catch (ClassNotFoundException cnfe) {
        throw new IllegalArgumentException("Cannot find implementing class for: " + attClass.getName());
      }      
    }
  }
  
  // Hack to rethrow unknown Exceptions from {@link MethodHandle#invoke}:
  // TODO: remove the impl in test-framework, this one is more elegant :-)
  static void rethrow(Throwable t) {
    AttributeFactory.<Error>rethrow0(t);
  }

  @SuppressWarnings("unchecked")
  private static <T extends Throwable> void rethrow0(Throwable t) throws T {
    throw (T) t;
  }
  
}
