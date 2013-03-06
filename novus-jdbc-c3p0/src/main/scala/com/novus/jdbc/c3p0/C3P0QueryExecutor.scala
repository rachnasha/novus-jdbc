package com.novus.jdbc.c3p0

import com.novus.jdbc.{QueryExecutor, Queryable}
import java.sql.Connection
import com.mchange.v2.c3p0.ComboPooledDataSource

/**
 * Implementation of the QueryExecutor using the C3P0 connection pool as the backing SQL connection pool. This class
 * does not handle exceptions other than to log that they happened. Regardless of the outcome of a query, returns the
 * connection back to the connection pool.
 *
 * @param pool a reference to a C3P0 connection pool.
 */
class C3P0QueryExecutor[DBType : Queryable](pool: ComboPooledDataSource) extends QueryExecutor[DBType]{

  /**
   * Execute some function requiring a connection, performing whatever management is necessary (eg ARM / loaner).
   */
  protected def managed[A](f: Connection => A): A ={
    val connection = pool.getConnection
    try{
      f(connection)
    }
    catch{
      case ex: Exception => log error ("%s, threw exception: %s" format(this, ex.getMessage)); throw ex
    }
    finally{
      if (connection != null) connection.close()
    }
  }

  /** Shuts down the underlying connection pool. Should be called before this object is garbage collected. */
  def shutdown() {
    pool close ()
  }

  override def toString = "C3P0QueryExecutor: " + pool.getDataSourceName
}

object C3P0QueryExecutor{
  def apply[DBType : Queryable](driver: String,
                                uri: String,
                                user: String,
                                password: String,
                                maxIdle: Int,
                                minPoolSize: Int,
                                maxPoolSize: Int): C3P0QueryExecutor[DBType] ={
    val pool = new ComboPooledDataSource()
    pool setDriverClass driver
    pool setJdbcUrl uri
    pool setUser user
    pool setPassword password
    pool setMaxIdleTime maxIdle
    pool setInitialPoolSize minPoolSize
    pool setMinPoolSize minPoolSize
    pool setMaxPoolSize maxPoolSize

    new C3P0QueryExecutor[DBType](pool)
  }

  /**
   * Creates a QueryExecutor with the C3P0 connection pool without explicit parameter passing in the constructor.
   * Pool parameters should be written to either the properties file or the XML configuration file otherwise will use
   * the hard-coded default parameters in the pool implementation itself.
   *
   * @param driver The driver to be used with the underlying connection pool
   */
  def apply[DBType : Queryable](driver: String, configName: String) ={
    val pool = new ComboPooledDataSource(configName)

    Class forName driver
    new C3P0QueryExecutor[DBType](pool)
  }
}