# mbg-paging-plugin ![ci](https://travis-ci.org/zhengzhiren/mbg-paging-plugin.svg?branch=master)
MyBatis Generator paging plugin for MySQL

This plugin generates the following methods for each mapping interface:
```java
List<Foo> selectByExampleWithPage(@Param("example") FooExample example,  @Param("offset") int offset, @Param("limit") int limit));
```

In the XxxMapper.xml files, it uses **limit offset, limit** clause for paging
