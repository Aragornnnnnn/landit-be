// 별도 FE 체크아웃의 기존 코드를 수정 없이 불러오는 통합 테스트 설정이다.
import { createRequire } from 'node:module';
import { dirname } from 'node:path';
const fe = process.env.LAN474_FE_ROOT;
const require = createRequire(`${fe}/apps/web/package.json`);
const renderer = createRequire(require.resolve('@testing-library/react'));
const aliases = [
  { find: 'next/navigation', replacement: require.resolve('next/navigation') },
  { find: '@', replacement: `${fe}/apps/web/src` },
  { find: /^react$/, replacement: renderer.resolve('react') },
  { find: /^react-dom$/, replacement: renderer.resolve('react-dom') },
  ...['@testing-library/react', '@tanstack/react-query', 'vitest'].map(name => ({ find: name, replacement: dirname(require.resolve(`${name}/package.json`)) })),
];
export default {
  root: dirname(new URL(import.meta.url).pathname),
  resolve: { alias: aliases, dedupe: ['react', 'react-dom', 'zustand'] },
  test: {
    environment: 'jsdom',
    include: ['payment-on.test.ts'],
    setupFiles: ['payment-on.setup.ts'],
    testTimeout: 25000,
    hookTimeout: 15000,
    reporters: ['verbose'],
    server: { deps: { inline: ['@tanstack/react-query', 'zustand'] } },
  },
};
