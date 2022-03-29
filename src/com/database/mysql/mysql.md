### MySQL 相关问题总结

### InnoDB 与 MyISAM 的区别

+ InnoDB支持事务，MyISA不支持事务
+ InnoDB支持外键，MyISAM不支持外键
+ InnoDB支持MVCC
+ select count(*) from table，MyISAM更快，因为其有一个变量保存了整个表的总行数
+ InnoDB支持表、行级锁，MyISAM支持表级锁
+ InnoDB必须有主键，MyISAM可以没有主键



###  数据库三大范式

+ 第一范式：数据表中的每一列(每个字段)都不可以再拆分
+ 第二范式：在第一范式的基础上，非主键列完全依赖于主键，而不能是依赖于主键的一部分
+ 第三范式：在满足第二范式的基础上，表中的非主键只依赖于主键，而不依赖于其他非主键
