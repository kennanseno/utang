import { Pool, type PoolClient, type QueryResultRow } from "pg";

const connectionString =
  process.env.DB_URL ??
  process.env.DATABASE_URL ??
  "postgresql://utang:utang@localhost:5432/utang";

// Enable SSL for hosted providers (e.g. Supabase). Local Postgres over
// localhost/127.0.0.1 does not require SSL.
const isLocalConnection = /@(localhost|127\.0\.0\.1)[:/]/.test(connectionString);
const ssl = isLocalConnection ? undefined : { rejectUnauthorized: false };

const globalPool = globalThis as typeof globalThis & {
  __utangPool?: Pool;
};

const pool =
  globalPool.__utangPool ??
  new Pool({
    connectionString,
    ssl,
  });

if (!globalPool.__utangPool) {
  globalPool.__utangPool = pool;
}

export async function query<T extends QueryResultRow>(
  text: string,
  params: unknown[] = []
) {
  return pool.query<T>(text, params);
}

export async function withTransaction<T>(
  run: (client: PoolClient) => Promise<T>
): Promise<T> {
  const client = await pool.connect();
  try {
    await client.query("BEGIN");
    const result = await run(client);
    await client.query("COMMIT");
    return result;
  } catch (error) {
    await client.query("ROLLBACK");
    throw error;
  } finally {
    client.release();
  }
}
