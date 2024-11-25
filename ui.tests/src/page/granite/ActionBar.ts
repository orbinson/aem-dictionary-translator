import { Locator } from "@playwright/test";

export class ActionBar {
    readonly locator: Locator;

    constructor(Locator: Locator) {
        this.locator = Locator;
    }

    getButton(trackingElement: string) {
        return this.locator.locator(`[trackingelement="${trackingElement}"]`);
    }
}