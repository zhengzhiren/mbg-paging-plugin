# mbg-paging-plugin
MyBatis Generator paging plugin for MySQL

This plugin generates the following methods for each mapping interface:
```java
List<Foo> selectByExampleWithRowbounds(@Param("example") FooExample example, @Param("rowBounds") RowBounds rowBounds);
```

In the XxxMapper.xml files, it uses **limit offset, limit** clause for paging
