import{d as c,z as d,a as u,c as m,f as a,t as s,g as i,T as r,K as b,x as f}from"./index-f3f720fc.js";const p=["aria-label"],y={class:"bg-white border-2 border-blue-800 border-solid box-border flex flex-col gap-[1rem] items-start modal-container px-[3.5rem] py-[2.5rem]"},x={class:"label-01-bold text-black"},T={class:"label-03-reg text-black"},v={class:"flex flex-row gap-[1rem] modal-buttons-container"},w=c({__name:"PopupModal",props:{ariaLabel:null,headerText:null,contentText:null,confirmText:null,cancelButtonType:null,confirmButtonType:null},emits:["closeModal","confirmAction"],setup(l){return d(()=>{const o=document.getElementsByClassName("popup-modal-wrapper")[0];o.focus();const e=".modal-buttons-container button",t=document.querySelectorAll(e);document.addEventListener("keydown",n=>{n.key==="Tab"&&(n.shiftKey?document.activeElement===o&&(t[t.length-1].focus(),n.preventDefault()):document.activeElement===t[t.length-1]&&(t[0].focus(),n.preventDefault()))})}),(o,e)=>(u(),m("div",{"aria-label":l.ariaLabel,class:"bg-background fixed flex h-full items-center justify-center left-0 popup-modal-wrapper top-0 w-screen z-999",role:"dialog",tabindex:"0",onClick:e[2]||(e[2]=b(t=>o.$emit("closeModal"),["self"])),onKeydown:e[3]||(e[3]=f(t=>o.$emit("closeModal"),["esc"]))},[a("div",y,[a("div",x,s(l.headerText),1),a("div",T,s(l.contentText),1),a("div",v,[i(r,{"button-type":l.cancelButtonType,label:"Abbrechen",onClick:e[0]||(e[0]=t=>o.$emit("closeModal"))},null,8,["button-type"]),i(r,{"button-type":l.confirmButtonType,label:l.confirmText,onClick:e[1]||(e[1]=t=>o.$emit("confirmAction"))},null,8,["button-type","label"])])])],40,p))}});export{w as _};
