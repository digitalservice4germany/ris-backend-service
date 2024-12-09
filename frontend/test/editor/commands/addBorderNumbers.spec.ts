import { CommandProps } from "@tiptap/core"
import { Schema, Node as ProsemirrorNode } from "prosemirror-model"
import { EditorState, Transaction } from "prosemirror-state"
import { describe, it, expect, vi, beforeEach, afterEach } from "vitest"
import { nextTick } from "vue"
import {
  BorderNumber,
  BorderNumberContent,
  BorderNumberNumber,
} from "@/editor/borderNumber"
import addBorderNumbers from "@/editor/commands/addBorderNumbers"
import borderNumberService from "@/services/borderNumberService"
import { createDocWithBorderNumber } from "~/test-helper/dataGenerators"

const schema = new Schema({
  nodes: {
    doc: { content: "block+" },
    paragraph: { content: "text*", group: "block" },
    text: { inline: true },
    customBlock: {
      content: "text*",
    },
    borderNumber: BorderNumber,
    borderNumberNumber: BorderNumberNumber,
    borderNumberContent: BorderNumberContent,
  },
})

vi.mock("vue", () => ({
  nextTick: vi.fn(() => Promise.resolve()),
}))

describe("addBorderNumbers command", () => {
  const dispatch = vi.fn()

  beforeEach(() => {
    vi.spyOn(
      borderNumberService,
      "makeBorderNumbersSequential",
    ).mockImplementation(() => vi.fn())
    dispatch.mockClear()
  })
  afterEach(() => vi.restoreAllMocks())

  it("should add borderNumber node and dispatch transaction", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: schema.nodes.doc.createAndFill({}, [
          schema.nodes.paragraph.create({}, schema.text("Test content")),
        ]) as ProsemirrorNode,
        schema,
      }),
      dispatch,
    }

    // Act
    const result = addBorderNumbers(commandProps as CommandProps)

    // Assert
    expect(result).toBe(true)
    expect(dispatch).toHaveBeenCalled()
    await nextTick()
    expect(borderNumberService.makeBorderNumbersSequential).toHaveBeenCalled()
  })

  it("should not modify the document if paragraph is empty", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: schema.nodes.doc.createAndFill() as ProsemirrorNode,
        schema,
      }),
      dispatch,
    }

    // Act
    const result = addBorderNumbers(commandProps as CommandProps)

    // Assert
    expect(result).toBe(false)
    expect(dispatch).not.toHaveBeenCalled()
  })

  it("should not modify the document if there are no nodes", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: schema.nodes.doc.create(),
        schema,
      }),
      dispatch,
    }

    // Act
    const result = addBorderNumbers(commandProps as CommandProps)

    // Assert
    expect(result).toBe(false)
    expect(dispatch).not.toHaveBeenCalled()
  })

  it("should not modify the document if there is already a border number", async () => {
    // Arrange
    const commandProps: {
      state: EditorState
      dispatch: ((tr: Transaction) => void) | undefined
    } = {
      state: EditorState.create({
        doc: createDocWithBorderNumber(schema),
        schema,
      }),
      dispatch,
    }

    // Act
    const result = addBorderNumbers(commandProps as CommandProps)

    // Assert
    expect(result).toBe(false)
    expect(dispatch).not.toHaveBeenCalled()
    await nextTick()
    expect(borderNumberService.makeBorderNumbersSequential).toHaveBeenCalled()
  })
})
