/**
# Database Connection Configuration
db.url=jdbc:mysql://localhost:3306/library_db
db.username=root
db.password=202304247edward_jr/2%%

        # Connection Pool Settings
pool.maximum.size=10
pool.minimum.idle=3
pool.connection.timeout=30000
pool.idle.timeout=600000
pool.max.lifetime=1800000

        # Performance Settings
pool.cache.prep.stmts=true
pool.prep.stmt.cache.size=250
pool.prep.stmt.cache.sql.limit=2048

        # Security Settings
db.use.ssl=false
db.allow.public.key.retrieval=true

        # Monitoring
pool.leak.detection.threshold=60000
 **/