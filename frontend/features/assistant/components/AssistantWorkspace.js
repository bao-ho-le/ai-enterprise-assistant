import AssistantSidebar from "./AssistantSidebar";

// App shell for /chat: the sidebar lives in the layout so it survives navigation between
// conversations; each page fills the remaining space (thread + documents dock).
export default function AssistantWorkspace({ children }) {
  return (
    <div className="flex flex-1 overflow-hidden">
      <AssistantSidebar />
      {children}
    </div>
  );
}
