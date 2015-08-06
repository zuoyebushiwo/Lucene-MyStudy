package zuoye;

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

public class Test {
  
  class Person {
    private String name;
    private int age;
    
    public String getName() {
      return name;
    }
    
    public void setName(String name) {
      this.name = name;
    }
    
    public int getAge() {
      return age;
    }
    
    public void setAge(int age) {
      this.age = age;
    }
  }
  
  private String privateInfo(){  
    return "10";  
}  
  
  public static void main(String[] args) {
    MethodInvokeTypes tt = new MethodInvokeTypes();
    tt.invoke();
    
    String a = "abcd";
    MethodType mt = MethodType.methodType(String.class, int.class, int.class);
    try {
      MethodHandle handle = MethodHandles.lookup().findVirtual(String.class,
          "substring", mt);
      System.out.println(handle.invoke(a, 1, 2));
      
      mt = MethodType.methodType(void.class, int.class, double.class);
      System.out.println(mt);
      System.out.println(mt.wrap());
      System.out.println(mt.unwrap());
      
      System.out.println(mt.generic());
      System.out.println(mt.toMethodDescriptorString());
      System.out.println(mt.erase());
      
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      MethodHandle mh = lookup.findConstructor(Person.class,
          MethodType.methodType(void.class, Test.class));
      Person p = (Person) mh.invokeExact(new Test());
      System.out.println(p);
      
      mh = lookup.findSpecial(Test.class, "privateInfo", MethodType.methodType(String.class), Test.class);
      System.out.println(mh.invoke(new Test()));
      
      mh=MethodHandles.constant(String.class, "hello");  
      System.out.println((String)mh.invokeExact()); 
      
      mh=MethodHandles.identity(String.class);  
      System.out.println((String)mh.invokeExact("hello"));
    } catch (Throwable e) {
      e.printStackTrace();
    } 
  }
  
}
