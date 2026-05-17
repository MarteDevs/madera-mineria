/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        // Paleta de colores del sistema de madera
        primary: {
          50:  '#fdf8f0',
          100: '#faefd8',
          500: '#c8811a',
          600: '#a86a14',
          700: '#8a5510',
          900: '#5c3809',
        },
        madera: {
          claro:  '#DEB887',
          medio:  '#A0522D',
          oscuro: '#5C3317',
        }
      }
    },
  },
  plugins: [],
}
