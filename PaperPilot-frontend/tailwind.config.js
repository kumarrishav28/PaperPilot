/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      boxShadow: {
        glow: '0 0 0 1px rgba(99, 102, 241, 0.2), 0 0 24px rgba(6, 182, 212, 0.18)',
      },
    },
  },
  plugins: [],
}
