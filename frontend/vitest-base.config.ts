import { defineConfig } from 'vitest/config';

export default defineConfig({
  test: {
    pool: 'threads',
    fileParallelism: false,
    poolTimeout: 120000,
    testTimeout: 30000,
    hookTimeout: 30000,
  },
});
