export async function retry(asyncFunc, validateFunc, retries, delayMs, ...args) {
    let lastResult;
    for (let attempt = 1; attempt <= retries; attempt++) {
        lastResult = await asyncFunc(args);

        if (attempt === retries || validateFunc(lastResult)) {
            return lastResult;
        }

        await new Promise(resolve => setTimeout(resolve, delayMs));
    }
}