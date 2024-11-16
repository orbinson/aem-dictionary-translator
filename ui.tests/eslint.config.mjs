import globals from "globals";
import pluginJs from "@eslint/js";
// enable when issue solved: https://github.com/eslint/eslint/issues/19134
// import tseslint from "typescript-eslint";

export default [
    {
        files: ["**/*.{js,mjs,cjs,ts}"],
        rules: {
            "object-curly-spacing": ["error", "always"],
            "quotes": ["error", "double"]
        },
        languageOptions: {
            globals: {
                ...globals.browser,
                ...globals.node
            },
        }
    },
    pluginJs.configs.recommended,
    // ...tseslint.configs.recommended,
];
