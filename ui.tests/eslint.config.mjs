import globals from "globals";
import pluginJs from "@eslint/js";
import tseslint from "typescript-eslint";

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
    ...tseslint.configs.recommended,
];
