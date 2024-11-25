import { Locator, Page } from "@playwright/test";
import { ActionBar } from "./ActionBar";
import { Table } from "./Table";

export class CollectionPage {
    readonly page: Page;
    readonly heading: Locator;
    readonly actionBar: ActionBar;
    readonly table: Table;

    constructor(page: Page) {
        this.page = page;

        this.heading = page.getByRole("heading");
        this.actionBar = new ActionBar(page.locator("#granite-shell-actionbar"));
        this.table = new Table(page.locator("table[is=coral-table]"))
    }

    async goto(url: string) {
        await this.page.goto(url);
    }

    getActionBarButton(label: string) {
        return this.actionBar.getButton(label);
    }

    getTableHeaderCell(position: number) {
        return this.table.getHeaderCell(position);
    }

    getTableCell(row: number, column: number) {
        return this.table.getCell(row, column);
    }
}