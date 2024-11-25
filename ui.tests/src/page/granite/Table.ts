import { Locator } from "@playwright/test";

export class Table {
    readonly locator: Locator;
    readonly header: Locator;
    readonly body: Locator;

    constructor(Locator: Locator) {
        this.locator = Locator;
        this.header = this.locator.locator("thead");
        this.body = this.locator.locator("tbody");
    }

    getHeaderCell(position: number) {
        return this.header.locator("th").nth(position);
    }

    getRow(position: number) {
        return this.body.locator("tr").nth(position);
    }

    getCell(row: number, column: number) {
        return this.getRow(row).locator("td").nth(column);
    }
}