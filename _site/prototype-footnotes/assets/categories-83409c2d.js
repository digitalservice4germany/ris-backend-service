import{d as y,a as d,c as f,r as z,X as A,x as W,t as $,F as T,V,f as i,g as v,b as D,$ as j,a0 as H,l as E,Z as S,A as R,e as ae,k as p,h as X,a1 as Oe,i as b,M as F,N as he,T as J,w as C,j as ge,m as be,_ as we,q,a2 as Ne,P as Ie,a3 as Ve,z as de,B as ke,H as Y,I as ye,a4 as Pe,a5 as ze,O as G,W as Re,a6 as Be,G as Ae,u as Ke}from"./index-a65e9a75.js";import{b as ie,c as Me,I as ue,D as je,C as He,u as Ge}from"./InputGroup-8b0df58a.js";import{s as le,c as fe,a as me}from"./coreDataFields-2fdf8160.js";import{_ as qe}from"./Pagination.vue_vue_type_script_setup_true_lang-dc7f4f3a.js";import{d as xe}from"./dayjs.min-633259b2.js";import{I as We}from"./types-7b85aa56.js";import{T as $e,s as Je}from"./fileService-9fd2a87e.js";import{_ as Ze}from"./DocumentUnitWrapper.vue_vue_type_script_setup_true_lang-c2637f78.js";import{O as Qe,_ as Xe,u as Ye}from"./SideToggle.vue_vue_type_script_setup_true_lang-bf3ce063.js";import{P as ee,d as re}from"./documentUnitService-a79eacc8.js";import"./FileInput-54257e4a.js";const et=["onClick","onKeyup"],tt=y({__name:"TokenizeText",props:{text:null,keywords:null},emits:["linkToken:clicked"],setup(e,{emit:t}){const s=e;function n(){return s.keywords.length===0?[{content:s.text,isLink:!1}]:s.text.split(new RegExp(`(${s.keywords.join("|")})`)).map(a=>({content:a,isLink:s.keywords.includes(a)}))}function o(a){a.isLink&&t("linkToken:clicked",a.content)}return(a,l)=>(d(!0),f(T,null,z(n(),(r,m)=>(d(),f("span",{key:m,class:A(r.isLink&&"linked-field"),onClick:u=>o(r),onKeyup:W(u=>o(r),["enter"])},$(r.content),43,et))),128))}});const De=V(tt,[["__scopeId","data-v-ce969719"]]),nt=e=>(j("data-v-785e8bb8"),e=e(),H(),e),st={class:"flex mt-20"},ot={class:"flex flex-col grow"},it={class:"flex"},lt={class:"flex label-02-reg pt-8 text-blue-800"},at=["aria-label"],dt={class:"ml-112 text-black"},rt={key:0},ct=["aria-label"],ut=nt(()=>i("hr",{class:"border-blue-500 mt-8 w-full"},null,-1)),ft=y({__name:"FieldOfLawListEntry",props:{fieldOfLaw:null,showBin:{type:Boolean}},emits:["remove-from-list","node-clicked","linkedField:clicked"],setup(e,{emit:t}){const s=e;function n(o){t("linkedField:clicked",o)}return(o,a)=>(d(),f(T,null,[i("div",st,[i("div",ot,[i("div",it,[i("div",lt,[i("span",{"aria-label":s.fieldOfLaw.identifier+" "+s.fieldOfLaw.text+" im Sachgebietsbaum anzeigen",class:"link mr-12 w-44 whitespace-nowrap",onClick:a[0]||(a[0]=l=>t("node-clicked")),onKeyup:a[1]||(a[1]=W(l=>t("node-clicked"),["enter"]))},$(s.fieldOfLaw.identifier),41,at),i("span",dt,[v(De,{keywords:s.fieldOfLaw.linkedFields??[],text:s.fieldOfLaw.text,"onLinkToken:clicked":n},null,8,["keywords","text"])])])])]),s.showBin?(d(),f("div",rt,[i("button",{"aria-label":e.fieldOfLaw.identifier+" "+e.fieldOfLaw.text+" entfernen",class:"material-icons text-blue-800",onClick:a[2]||(a[2]=l=>t("remove-from-list"))}," delete_outline ",8,ct)])):D("",!0)]),ut],64))}});const Ue=V(ft,[["__scopeId","data-v-785e8bb8"]]),mt={class:"pt-20"},pt={key:0},vt={key:1},_t=i("hr",{class:"border-blue-500 mt-20 w-full"},null,-1),ht=y({__name:"FieldOfLawSelectionList",props:{selectedFieldsOfLaw:null},emits:["remove-from-list","node-clicked","linkedField:clicked"],setup(e,{emit:t}){const s=e;return(n,o)=>(d(),f("div",mt,[s.selectedFieldsOfLaw.length?(d(),f("div",vt,[_t,(d(!0),f(T,null,z(s.selectedFieldsOfLaw,a=>(d(),E(Ue,{key:a.identifier,"field-of-law":a,"show-bin":"","onLinkedField:clicked":o[0]||(o[0]=l=>t("node-clicked",l)),onNodeClicked:l=>t("node-clicked",a.identifier),onRemoveFromList:l=>t("remove-from-list",a.identifier)},null,8,["field-of-law","onNodeClicked","onRemoveFromList"]))),128))])):(d(),f("div",pt," Die Liste ist aktuell leer "))]))}}),Q="root";function gt(){return{identifier:Q,text:"Alle Sachgebiete anzeigen",children:[],childrenCount:17,norms:[],isExpanded:!1}}function bt(e){const t=[],s=(n,o)=>{o.push(n),n.children.forEach(a=>s(a,o))};return s(e,t),t}const M={async getSelectedFieldsOfLaw(e){const t=await S.get(`caselaw/documentunits/${e}/contentrelatedindexing/fieldsoflaw`);return t.status>=300&&(t.error={title:`Sachgebiete für die Dokumentationseinheit ${e} konnten nicht geladen werden.`}),t},async addFieldOfLaw(e,t){const s=await S.put(`caselaw/documentunits/${e}/contentrelatedindexing/fieldsoflaw/${t}`);return s.status>=300&&(s.error={title:`Sachgebiet ${t} konnte nicht zu
          Dokumentationseinheit ${e} hinzugefügt werden`}),s},async removeFieldOfLaw(e,t){const s=await S.delete(`caselaw/documentunits/${e}/contentrelatedindexing/fieldsoflaw/${t}`);return s.status>=300&&(s.error={title:`Sachgebiet ${t} konnte nicht von der
        Dokumentationseinheit ${e} entfernt werden`}),s},async getChildrenOf(e){const t=await S.get(`caselaw/fieldsoflaw/${e}/children`);return t.status>=300&&(t.error={title:"Sachgebiete unterhalb von "+e+" konnten nicht geladen werden."}),t},async getTreeForIdentifier(e){const t=await S.get(`caselaw/fieldsoflaw/${e}/tree`);return t.status>=300&&(t.error={title:"Pfad zu ausgewähltem Sachgebiet konnte nicht geladen werden."}),t},async searchForFieldsOfLaw(e,t,s){const n=await S.get(`caselaw/fieldsoflaw?pg=${e}&sz=${t}`,{params:{q:s??""}});return n.status>=300&&(n.error={title:"Die Suche nach Sachgebieten konnte nicht ausgeführt werden."}),n}},wt={class:"flex flex-row"},kt={key:0,class:"pl-24"},yt={key:1},xt=["aria-label"],$t={key:2},Dt=["aria-label"],Ut={key:0,"aria-label":"Sachgebiet entfernen",class:"material-icons selected-icon"},Lt={class:"flex flex-col"},St={class:"flex flex-row"},Ct={key:0,class:"identifier pl-8"},Ft={class:"font-size-14px pl-6 pt-2 text-blue-800"},Tt={key:0,class:"flex flex-col pb-6 pl-8"},Et={class:"flex flex-row flex-wrap font-size-14px norms-font-color"},Ot={key:0},Nt=y({__name:"FieldOfLawNodeComponent",props:{selectedNodes:null,node:null,selected:{type:Boolean},showNorms:{type:Boolean}},emits:["node:toggle","node:select","node:unselect","linkedField:clicked"],setup(e,{emit:t}){const s=e,n=R(()=>s.node);function o(r){t("linkedField:clicked",r)}function a(){return n.value.childrenCount>n.value.children.length}async function l(){if(a()){let r;n.value.children.length>0&&(r=Oe(n.value.children[0])),await M.getChildrenOf(n.value.identifier).then(m=>{if(!m.data||(n.value.children=m.data,!r))return;const u=n.value.children.find(h=>h.identifier===r.identifier);u&&(u.children=r.children,u.isExpanded=!0,u.inDirectPathMode=!0)})}n.value.inDirectPathMode?n.value.inDirectPathMode=!1:n.value.isExpanded=!n.value.isExpanded}return(r,m)=>{const u=ae("FieldOfLawNodeComponent",!0);return d(),f("div",{class:A(["flex flex-col",p(n).identifier!==p(Q)?"pl-36":""])},[i("div",wt,[p(n).childrenCount===0?(d(),f("div",kt)):(d(),f("div",yt,[i("button",{"aria-label":p(n).identifier+" "+p(n).text+" aufklappen",class:"bg-blue-200 material-icons rounded-full text-blue-800 w-icon",onClick:l},$(a()||!s.node.isExpanded?"add":"remove"),9,xt)])),p(n).identifier!==p(Q)?(d(),f("div",$t,[i("button",{"aria-label":p(n).identifier+" "+p(n).text+(e.selected?" entfernen":" hinzufügen"),class:"align-top appearance-none border-2 focus:outline-2 h-24 hover:outline-2 ml-12 outline-0 outline-blue-800 outline-none outline-offset-[-4px] rounded-sm text-blue-800 w-24",onClick:m[0]||(m[0]=h=>e.selected?t("node:unselect",p(n).identifier):t("node:select",p(n)))},[e.selected?(d(),f("span",Ut," done ")):D("",!0)],8,Dt)])):D("",!0),i("div",null,[i("div",Lt,[i("div",St,[p(n).identifier!==p(Q)?(d(),f("div",Ct,$(p(n).identifier),1)):D("",!0),i("div",Ft,[v(De,{keywords:s.node.linkedFields??[],text:s.node.text,"onLinkToken:clicked":o},null,8,["keywords","text"])])])]),e.showNorms?(d(),f("div",Tt,[i("div",Et,[(d(!0),f(T,null,z(p(n).norms,(h,g)=>(d(),f("span",{key:g},[i("strong",null,$(h.singleNormDescription),1),X(" "+$(h.abbreviation)+$(g<p(n).norms.length-1?", ":""),1)]))),128))])])):D("",!0)])]),p(n).isExpanded&&p(n).children.length?(d(),f("div",Ot,[(d(!0),f(T,null,z(p(n).children,h=>(d(),E(u,{key:h.identifier,node:h,selected:s.selectedNodes.some(({identifier:g})=>g===h.identifier),"selected-nodes":e.selectedNodes,"show-norms":e.showNorms,"onLinkedField:clicked":m[1]||(m[1]=g=>t("linkedField:clicked",g)),"onNode:select":m[2]||(m[2]=g=>t("node:select",g)),"onNode:unselect":m[3]||(m[3]=g=>t("node:unselect",g))},null,8,["node","selected","selected-nodes","show-norms"]))),128))])):D("",!0)],2)}}});const It=V(Nt,[["__scopeId","data-v-64ba0db4"]]),Le=e=>(j("data-v-ca0f9eab"),e=e(),H(),e),Vt={class:"flex items-center justify-between pb-10"},Pt=Le(()=>i("h1",{class:"heading-03-regular"},"Sachgebietsbaum",-1)),zt={class:"flex items-center"},Rt={key:0,"aria-label":"Sachgebiet entfernen",class:"material-icons selected-icon"},Bt=Le(()=>i("span",{class:"pl-8"},"Normen anzeigen",-1)),At=y({__name:"FieldOfLawTree",props:{selectedNodes:null,clickedIdentifier:null,showNorms:{type:Boolean}},emits:["add-to-list","remove-from-list","reset-clicked-node","toggle-show-norms","linkedField:clicked"],setup(e,{emit:t}){const s=e,n=b(gt()),o=R(()=>s.clickedIdentifier);F(o,()=>m(o.value));function a(u){t("add-to-list",u.identifier)}function l(u){t("remove-from-list",u)}function r(u){t("linkedField:clicked",u)}const m=async u=>{if(!u)return;const h=await M.getTreeForIdentifier(u);h.data&&(n.value.children=[h.data],bt(n.value).forEach(g=>{g.isExpanded=!0,g.inDirectPathMode=!0}),t("reset-clicked-node"))};return(u,h)=>(d(),f(T,null,[i("div",Vt,[Pt,i("div",zt,[i("button",{"aria-label":"Normen anzeigen",class:"align-top appearance-none border-2 focus:outline-2 h-24 hover:outline-2 outline-0 outline-blue-800 outline-none outline-offset-[-4px] rounded-sm text-blue-800 w-24",onClick:h[0]||(h[0]=g=>t("toggle-show-norms"))},[e.showNorms?(d(),f("span",Rt," done ")):D("",!0)]),Bt])]),(d(),E(It,{key:n.value.identifier,node:n.value,selected:s.selectedNodes.some(({identifier:g})=>g===n.value.identifier),"selected-nodes":e.selectedNodes,"show-norms":e.showNorms,"onLinkedField:clicked":r,"onNode:select":a,"onNode:unselect":l},null,8,["node","selected","selected-nodes","show-norms"]))],64))}});const Kt=V(At,[["__scopeId","data-v-ca0f9eab"]]),Mt=i("p",{class:"heading-04-regular pb-8 pt-24"},"Direkteingabe Sachgebiet",-1),jt={class:"flex flex-col w-1/3"},Ht={class:"flex flex-row items-stretch"},Gt={class:"grow"},qt=y({__name:"FieldOfLawDirectInputSearch",emits:["add-to-list"],setup(e,{emit:t}){function s(n){n&&t("add-to-list",n.label)}return(n,o)=>(d(),f(T,null,[Mt,i("div",jt,[i("div",Ht,[i("div",Gt,[v(ie,{id:"directInputCombobox","aria-label":"Direkteingabe-Sachgebietssuche eingeben","clear-on-choosing-item":"","item-service":p(le).getFieldOfLawSearchByIdentifier,placeholder:"Sachgebiet","onUpdate:modelValue":s},null,8,["item-service"])])])])],64))}}),Wt=i("h1",{class:"heading-03-regular pb-8"},"Suche",-1),Jt={class:"flex flex-col"},Zt={class:"pb-28"},Qt={class:"flex flex-row items-stretch"},Xt={class:"grow"},Yt={class:"pl-8"},en=y({__name:"FieldOfLawSearch",emits:["linkedField:clicked","node-clicked","do-show-norms"],setup(e,{emit:t}){const s=b(""),n=b(),o=b(),a=10;async function l(r){var u;console.log(r);const m=await M.searchForFieldsOfLaw(r,a,s.value);m.data?(o.value=m.data,n.value=m.data.content,(u=n.value)!=null&&u[0]&&t("node-clicked",n.value[0].identifier),s.value.includes("norm:")&&t("do-show-norms")):(o.value=void 0,n.value=void 0,console.error("Error searching for Nodes"))}return(r,m)=>(d(),f(T,null,[Wt,i("div",Jt,[i("div",Zt,[i("div",Qt,[i("div",Xt,[v(he,{id:"FieldOfLawSearchTextInput",modelValue:s.value,"onUpdate:modelValue":m[0]||(m[0]=u=>s.value=u),"aria-label":"Sachgebiete Suche","full-height":"",onEnterReleased:m[1]||(m[1]=u=>l(0))},null,8,["modelValue"])]),i("div",Yt,[v(J,{"aria-label":"Sachgebietssuche ausführen","button-type":"secondary",class:"w-fit",label:"Suchen",onClick:m[2]||(m[2]=u=>l(0))})])])]),o.value?(d(),E(qe,{key:0,"navigation-position":"bottom",page:o.value,onUpdatePage:l},{default:C(()=>[(d(!0),f(T,null,z(n.value,(u,h)=>(d(),E(Ue,{key:h,"field-of-law":u,"onLinkedField:clicked":m[3]||(m[3]=g=>t("node-clicked",g)),onNodeClicked:g=>t("node-clicked",u.identifier)},null,8,["field-of-law","onNodeClicked"]))),128))]),_:1},8,["page"])):D("",!0)])],64))}}),tn={class:"w-full"},nn={class:"flex flex-row"},sn={class:"bg-white flex flex-1 flex-col p-20"},on={class:"bg-white flex-1 p-20"},ln=i("hr",{class:"border-blue-700 w-full"},null,-1),an={class:"bg-white p-20"},dn=i("h1",{class:"heading-03-regular pb-8"},"Ausgewählte Sachgebiete",-1),rn=y({__name:"FieldOfLawMain",props:{documentUnitUuid:null},async setup(e){let t,s;const n=e,o=b([]),a=b(""),l=b(!1),r=([t,s]=ge(()=>M.getSelectedFieldsOfLaw(n.documentUnitUuid)),t=await t,s(),t);r.data&&(o.value=r.data);const m=async c=>{const w=await M.addFieldOfLaw(n.documentUnitUuid,c);w.data&&(o.value=w.data)},u=async c=>{const w=await M.removeFieldOfLaw(n.documentUnitUuid,c);w.data&&(o.value=w.data)};function h(c){a.value=c}function g(){a.value=""}function O(c){a.value=c}function I(c){setTimeout(()=>{a.value=c},20)}function U(c){return q("div",[q("span",{class:"text-blue-800",onClick:()=>I(c.identifier)},c.identifier),", "+c.text])}const _=be(U);return(c,w)=>(d(),E(we,{"as-column":"","data-set":o.value,"summary-component":p(_),title:"Sachgebiete"},{default:C(()=>[i("div",tn,[i("div",nn,[i("div",sn,[v(en,{"show-norms":l.value,onDoShowNorms:w[0]||(w[0]=L=>l.value=!0),onNodeClicked:h},null,8,["show-norms"])]),i("div",on,[v(Kt,{"clicked-identifier":a.value,"selected-nodes":o.value,"show-norms":l.value,onAddToList:m,"onLinkedField:clicked":O,onRemoveFromList:u,onResetClickedNode:g,onToggleShowNorms:w[1]||(w[1]=L=>l.value=!l.value)},null,8,["clicked-identifier","selected-nodes","show-norms"])])]),ln,i("div",an,[dn,v(qt,{onAddToList:m}),v(ht,{"selected-fields-of-law":o.value,"onLinkedField:clicked":O,onNodeClicked:h,onRemoveFromList:u},null,8,["selected-fields-of-law"])])])]),_:1},8,["data-set","summary-component"]))}}),se={async getKeywords(e){const t=await S.get(`caselaw/documentunits/${e}/contentrelatedindexing/keywords`);return t.status>=300&&(t.error={title:"Schlagwörter konnten nicht geladen werden."}),t},async addKeyword(e,t){const s=encodeURIComponent(t),n=`caselaw/documentunits/${e}/contentrelatedindexing/keywords/${s}`,o=await S.put(n);return o.status>=300&&(o.error={title:`Schlagwort ${t} konnte nicht hinzugefügt werden`}),o},async deleteKeyword(e,t){const s=encodeURIComponent(t),n=`caselaw/documentunits/${e}/contentrelatedindexing/keywords/${s}`,o=await S.delete(n);return o.status>=300&&(o.error={title:`Schlagwort ${t} konnte nicht entfernt werden`}),o}},cn=e=>(j("data-v-76ed8a33"),e=e(),H(),e),un=["id","aria-label","onKeypress","onKeyup"],fn={key:0,class:"flex flex-row items-center"},mn=cn(()=>i("span",{class:"leading-default material-icons text-gray-900"},"error_outline",-1)),pn={class:"label-02-reg m-4 text-gray-900"},vn=y({__name:"KeywordsChipsInput",props:{id:null,value:null,modelValue:null,error:null,ariaLabel:null,placeholder:null,validationError:null},emits:["update:modelValue","addChip","deleteChip","input"],setup(e,{emit:t}){const s=e,{emitInputEvent:n}=Ne(s,t),o=b(s.modelValue??[]),a=b(),l=b(""),r=b(),m=b();function u(){t("update:modelValue",o.value.length===0?void 0:o.value)}function h(){const _=l.value.trim();if(_.length>0){if(o.value.includes(_)){a.value={title:"Schlagwort bereits vergeben."},l.value="";return}t("addChip",_),a.value||(o.value.push(_),u()),l.value=""}}function g(_){t("deleteChip",_)}const O=()=>{a.value=void 0},I=()=>{r.value!==void 0&&l.value===""&&r.value.focusFirst()},U=()=>{r.value!==void 0&&r.value.resetFocus(),m.value!==void 0&&m.value.focus()};return F(s,()=>{s.modelValue&&(o.value=s.modelValue),a.value=s.error}),F(o,()=>{o.value===void 0&&U()}),F(l,()=>{a.value&&l.value!==""&&(a.value=void 0)}),(_,c)=>{var w;return d(),f("div",null,[Ie(i("input",{id:e.id,ref_key:"chipsInput",ref:m,"onUpdate:modelValue":c[0]||(c[0]=L=>l.value=L),"aria-label":e.ariaLabel,class:"input mb-[0.5rem]",type:"text",onBlur:O,onInput:c[1]||(c[1]=(...L)=>p(n)&&p(n)(...L)),onKeypress:W(h,["enter"]),onKeyup:W(I,["right"])},null,40,un),[[Ve,l.value]]),a.value?(d(),f("div",fn,[mn,i("p",pn,$((w=a.value)==null?void 0:w.title),1)])):D("",!0),v(Me,{ref_key:"chipsList",ref:r,modelValue:o.value,"onUpdate:modelValue":c[2]||(c[2]=L=>o.value=L),error:a.value,onDeleteChip:g,onPreviousClickedOnFirst:U},null,8,["modelValue","error"])])}}});const _n=V(vn,[["__scopeId","data-v-76ed8a33"]]),hn={class:"bg-white mb-[2rem] p-16"},gn=i("h2",{class:"label-02-bold mb-[1rem]"},"Schlagwörter",-1),bn={class:"flex flex-row"},wn={class:"flex-1"},kn=y({__name:"KeyWords",props:{documentUnitUuid:null},setup(e){const t=e,s=b([]),n=b(),o=async l=>{if(l!==void 0){const r=await se.addKeyword(t.documentUnitUuid,l);if(r.error){n.value=r.error;return}r.data&&(s.value=r.data)}},a=async l=>{if(l!==void 0){const r=await se.deleteKeyword(t.documentUnitUuid,l);if(r.error){n.value=r.error;return}r.data&&(s.value=r.data)}};return F(t,async()=>{const l=await se.getKeywords(t.documentUnitUuid);if(l.error){n.value=l.error;return}l.data&&(s.value=l.data)},{immediate:!0}),(l,r)=>(d(),f("div",hn,[gn,i("div",bn,[i("div",wn,[v(_n,{id:"keywords","aria-label":"Schlagwörter",error:n.value,"model-value":s.value,onAddChip:o,onDeleteChip:a},null,8,["error","model-value"])])])]))}}),yn={class:"mb-[4rem]"},xn=i("h1",{class:"heading-02-regular mb-[1rem]"},"Inhaltliche Erschließung",-1),$n=y({__name:"DocumentUnitContentRelatedIndexing",props:{documentUnit:null},setup(e){const t=e;return(s,n)=>(d(),f("div",yn,[xn,v(kn,{"document-unit-uuid":t.documentUnit.uuid},null,8,["document-unit-uuid"]),v(rn,{"document-unit-uuid":t.documentUnit.uuid},null,8,["document-unit-uuid"])]))}});var K=(e=>(e[e.BEFORE_UPDATE=0]="BEFORE_UPDATE",e[e.ON_UPDATE=1]="ON_UPDATE",e[e.SUCCEED=200]="SUCCEED",e[e.ERROR=400]="ERROR",e))(K||{});const te=e=>(j("data-v-a08cd0d6"),e=e(),H(),e),Dn={class:"save-button-container"},Un={key:0,class:"save-status"},Ln={key:0},Sn=te(()=>i("div",{class:"icon"},[i("span",{class:"material-icons"}," cloud_upload ")],-1)),Cn=te(()=>i("p",{class:"status-text"},"Daten werden gespeichert",-1)),Fn=[Sn,Cn],Tn={key:1},En=te(()=>i("div",{class:"icon icon--error"},[i("span",{class:"material-icons"}," error_outline ")],-1)),On=te(()=>i("p",{class:"error-text"},"Fehler beim Speichern",-1)),Nn=[En,On],In={key:2},Vn={class:"status-text"},Pn={class:"on-succeed"},zn=y({__name:"SaveDocumentUnitButton",props:{ariaLabel:null,updateStatus:null},emits:["updateDocumentUnit"],setup(e,{emit:t}){const s=e,n=b(!1),o=b(!1),a=b(!0),l=b(!1),r=b(""),m=()=>{n.value=!1,o.value=!1,a.value=!1,l.value=!1},u=()=>{switch(m(),s.updateStatus){case K.BEFORE_UPDATE:{n.value=!0;return}case K.ON_UPDATE:{n.value=!1,o.value=!0;return}case K.SUCCEED:{n.value=!1,a.value=!0,r.value=xe().format("HH:mm:ss");return}default:n.value=!1,l.value=!0;return}},h=()=>{t("updateDocumentUnit")};return F(()=>s.updateStatus,()=>{u()}),de(()=>{u()}),(g,O)=>(d(),f("div",Dn,[v(J,{"aria-label":e.ariaLabel,label:"Speichern",onClick:h},null,8,["aria-label"]),n.value?D("",!0):(d(),f("div",Un,[o.value?(d(),f("div",Ln,Fn)):D("",!0),l.value?(d(),f("div",Tn,Nn)):D("",!0),a.value?(d(),f("div",In,[i("p",Vn,[X(" Zuletzt gespeichert um "),i("span",Pn,$(r.value),1),X(" Uhr ")])])):D("",!0)]))]))}});const Se=V(zn,[["__scopeId","data-v-a08cd0d6"]]);function P(e,t,s){return{name:e,label:t,fieldSize:s}}const Rn=[P("decisionName","Entscheidungsname","small"),P("headline","Titelzeile","small"),P("guidingPrinciple","Leitsatz","medium"),P("headnote","Orientierungssatz","small"),P("tenor","Tenor","medium"),P("reasons","Gründe","large"),P("caseFacts","Tatbestand","large"),P("decisionReasons","Entscheidungsgründe","large")];function pe(e){return e.charAt(0).toUpperCase()+e.slice(1)}function ve(e){return e.charAt(0).toLowerCase()+e.slice(1)}function Bn(e,t){return"nestedInputOf"+pe(e)+"And"+pe(t)}function An(e){const t=/^nestedInputOf(.*)And(.*)/g.exec(e);if(t)return{parentKey:ve(t[1]),childKey:ve(t[2])};throw new Error("Could not extract keys from neste input key")}function Kn(e,t,s){const n={...e};delete n[t],delete n[s];const o=Bn(t,s);return Object.assign(n,{[o]:{fields:{parent:e[t],child:e[s]}}}),n}function Mn(e){const t={...e};for(const[s,n]of Object.entries(e))if(typeof n=="object"&&n!==null&&"fields"in n&&"parent"in n.fields&&"child"in n.fields){const{parentKey:o,childKey:a}=An(s);delete t[s],t[o]=n.fields.parent,t[a]=n.fields.child}return t}function _e(e,t,s){return R({get:()=>{let n=e.value;return t.filter(o=>o.type===We.NESTED).forEach(o=>{n=Kn(n,o.inputAttributes.fields.parent.name,o.inputAttributes.fields.child.name)}),n},set:n=>{const o=Mn(n);s("update:modelValue",o)}})}const jn={key:0},Hn={key:1,class:"mb-[4rem]"},Gn=i("h1",{class:"core-data heading-02-regular mb-[1rem]"},"Stammdaten",-1),qn=i("div",{class:"mt-4"},"* Pflichtfelder zum Veröffentlichen",-1),Wn=y({__name:"DocumentUnitCoreData",props:{modelValue:null,updateStatus:null,validationErrors:null},emits:["updateDocumentUnit","update:modelValue"],setup(e,{emit:t}){const s=e,{modelValue:n}=ke(s),o=_e(n,me,t),a=_e(n,fe,t),l=b(),r=R(()=>l.value<600?1:2);de(()=>{const u=document.querySelector(".core-data");u!=null&&m.observe(u)});const m=new ResizeObserver(u=>{for(const h of u)l.value=h.contentRect.width});return(u,h)=>p(n)?(d(),f("div",Hn,[Gn,v(ue,{modelValue:p(a),"onUpdate:modelValue":h[0]||(h[0]=g=>Y(a)?a.value=g:null),"column-count":1,fields:p(fe),"validation-errors":s.validationErrors},null,8,["modelValue","fields","validation-errors"]),v(ue,{modelValue:p(o),"onUpdate:modelValue":h[1]||(h[1]=g=>Y(o)?o.value=g:null),"column-count":p(r),fields:p(me),"validation-errors":s.validationErrors},null,8,["modelValue","column-count","fields","validation-errors"]),qn,v(Se,{"aria-label":"Stammdaten Speichern Button",class:"mt-8","update-status":e.updateStatus,onUpdateDocumentUnit:h[2]||(h[2]=g=>t("updateDocumentUnit"))},null,8,["update-status"])])):(d(),f("div",jn,"Loading..."))}}),Jn={class:"mb-[4rem]"},Zn=i("h1",{class:"heading-02-regular mb-[1rem]"},"Kurz- & Langtexte",-1),Qn={class:"flex flex-col gap-36"},Xn=["for"],Yn=y({__name:"DocumentUnitTexts",props:{texts:null,updateStatus:null},emits:["updateValue","updateDocumentUnit"],setup(e,{emit:t}){const s=e,n=R(()=>Rn.map(o=>({id:o.name,name:o.name,label:o.label,aria:o.label,fieldSize:o.fieldSize,value:s.texts[o.name]})));return(o,a)=>(d(),f("div",Jn,[Zn,i("div",Qn,[(d(!0),f(T,null,z(p(n),l=>(d(),f("div",{key:l.id,class:""},[i("label",{class:"label-02-regular mb-2",for:l.id},$(l.label),9,Xn),v($e,{id:l.id,"aria-label":l.aria,class:"outline outline-2 outline-blue-900",editable:"","field-size":l.fieldSize,value:l.value,onUpdateValue:r=>t("updateValue",[l.id,r])},null,8,["id","aria-label","field-size","value","onUpdateValue"])]))),128)),v(Se,{"aria-label":"Kurz- und Langtexte Speichern Button","update-status":e.updateStatus,onUpdateDocumentUnit:a[0]||(a[0]=l=>t("updateDocumentUnit"))},null,8,["update-status"])])]))}}),ne=e=>(j("data-v-ebec96d3"),e=e(),H(),e),es=ne(()=>i("div",{class:"flex items-center"},[i("h2",{class:"grow heading-02-regular"},"Originaldokument")],-1)),ts={key:0,class:"flex flex-col gap-24"},ns=ne(()=>i("span",{class:"material-icons odoc-upload-icon"},"cloud_upload",-1)),ss=ne(()=>i("span",{class:"material-icons"},"arrow_forward",-1)),os=ne(()=>i("span",null,"Zum Upload",-1)),is={key:1},ls={inheritAttrs:!1},as=y({...ls,__name:"OriginalFileSidePanel",props:{open:{type:Boolean},hasFile:{type:Boolean},file:null,fixedPanelPosition:{type:Boolean}},emits:["update:open"],setup(e,{emit:t}){const s=e,n=b(!1);F(()=>s.open,()=>n.value=s.open??!1,{immediate:!0}),F(n,()=>t("update:open",n.value));const o=ye(),a=R(()=>({name:"caselaw-documentUnit-:documentNumber-files",params:{documentNumber:o.params.documentNumber},query:o.query}));return(l,r)=>{const m=ae("router-link");return d(),E(Xe,{"is-expanded":n.value,"onUpdate:isExpanded":r[0]||(r[0]=u=>n.value=u),label:"Originaldokument","opening-direction":p(Qe).LEFT},{default:C(()=>[i("div",Pe(ze(l.$attrs)),[i("div",{class:A(["basis-1/3! flex flex-col gap-56",{sticky:e.fixedPanelPosition}])},[es,e.hasFile?e.file?(d(),f("div",{key:2,class:A(["border-1 border-gray-400 border-solid overflow-scroll",{"editor-height":e.fixedPanelPosition}])},[v($e,{"element-id":"odoc","field-size":"max",value:e.file},null,8,["value"])],2)):(d(),f("div",is,"Dokument wird geladen")):(d(),f("div",ts,[ns,X(" Es wurde noch kein Originaldokument hochgeladen. "),v(m,{class:"flex gap-2 items-center link-01-bold",to:p(a)},{default:C(()=>[ss,os]),_:1},8,["to"])]))],2)],16)]),_:1},8,["is-expanded","opening-direction"])}}});const ds=V(as,[["__scopeId","data-v-ebec96d3"]]),rs={class:"underline"},cs={key:1,class:"label-02-reg"},Ce=y({__name:"InlineDecision",props:{decision:null},setup(e){return(t,s)=>{const n=ae("router-link");return d(),f("div",null,[e.decision.hasLink?(d(),E(n,{key:0,class:"link-01-bold underline",tabindex:"-1",target:"_blank",to:{name:"caselaw-documentUnit-:documentNumber-categories",params:{documentNumber:e.decision.documentNumber}}},{default:C(()=>[i("button",rs,$(e.decision.renderDecision),1)]),_:1},8,["to"])):(d(),f("span",cs,$(e.decision.renderDecision),1))])}}}),us={class:"flex flex-col items-start text-start w-full"},fs=["onClick","onKeyup"],ms=y({__name:"DecisionList",props:{decisions:null},emits:["removeLink"],setup(e,{emit:t}){const s=e,n=R(()=>[...s.decisions].sort((o,a)=>xe(a.date).diff(o.date)));return(o,a)=>(d(),f("div",us,[(d(!0),f(T,null,z(p(n),l=>(d(),f("div",{key:l.uuid,class:"border-b-1 border-b-blue-500 first:pt-0 flex flex-start justify-between label-02-reg last:border-none last:pb-0 py-10 w-full"},[v(Ce,{decision:l},null,8,["decision"]),i("span",{"aria-label":"Löschen",class:"cursor-pointer font-base icon material-icons ml-[1.5rem] text-blue-800",tabindex:"0",onClick:r=>t("removeLink",l),onKeyup:W(r=>t("removeLink",l),["enter"])},"delete_outline",40,fs)]))),128))]))}}),ps={class:"label-02-bold"},vs={class:"table"},_s={class:"table-cell"},hs={class:"p-8 table-cell"},gs={key:0,class:"bg-green-700 label-03-reg ml-24 px-24 py-4 rounded-full text-white"},bs=y({__name:"SearchResultList",props:{searchResults:null},emits:["linkDecision"],setup(e,{emit:t}){const s=e,n=b("Noch keine Suchparameter eingegeben");return F(s,()=>{s.searchResults&&(s.searchResults.length>0?n.value=`Suche hat ${s.searchResults.length} Treffer ergeben`:n.value="Suche hat keine Treffer ergeben")}),(o,a)=>(d(),f("div",null,[i("span",ps,$(n.value),1),i("div",vs,[(d(!0),f(T,null,z(e.searchResults,l=>(d(),f("div",{key:l.decision.uuid,class:"mb-24 mt-12 table-row"},[i("div",_s,[v(Ce,{decision:l.decision},null,8,["decision"])]),i("div",hs,[v(J,{"aria-label":"Treffer übernehmen",class:"ml-24",disabled:l.isLinked,label:"Übernehmen",onClick:r=>t("linkDecision",l.decision.uuid)},null,8,["disabled","onClick"])]),l.isLinked?(d(),f("span",gs,"Bereits hinzugefügt")):D("",!0)]))),128))])]))}}),oe={async createProceedingDecision(e,t){const s=await S.put(`caselaw/documentunits/${e}/proceedingdecisions`,{headers:{Accept:"application/json","Content-Type":"application/json"}},t);return s.status>=300?{status:500,error:{title:`Vorgehende Entscheidung konnte nicht zu
          Dokumentationseinheit ${e} hinzugefügt werden`}}:{status:200,data:s.data.map(n=>new ee({...n}))}},async linkProceedingDecision(e,t){const s=await S.put(`caselaw/documentunits/${e}/proceedingdecisions/${t}`);return s.status>=300?{status:500,error:{title:`Vorgehende Entscheidung ${t} konnte der Dokumentationseinheit ${e} nicht hinzugefügt werden.`}}:{status:200,data:s.data.proceedingDecisions.map(n=>new ee({...n}))}},async removeProceedingDecision(e,t){const s=await S.delete(`caselaw/documentunits/${e}/proceedingdecisions/${t}`);return s.status>=300&&(s.error={title:`Vorgehende Entscheidung ${t} für die Dokumentationseinheit ${e} konnten nicht entfernt werden.`}),s}},ws=e=>(j("data-v-2b7159a9"),e=e(),H(),e),ks={class:"mb-[4rem]"},ys=ws(()=>i("h1",{class:"heading-02-regular mb-[1rem]"},"Rechtszug",-1)),xs={class:"fake-input-group"},$s={class:"fake-input-group__row pb-32"},Ds={class:"fake-input-group__row__field flex-col"},Us={class:"fake-input-group__row pb-32"},Ls={class:"mb-10 mt-20"},Ss=y({__name:"ProceedingDecisions",props:{documentUnitUuid:null,proceedingDecisions:null},setup(e){const t=e,s=b(),n=b(),o=b(new ee);function a({court:_,date:c,fileNumber:w,documentType:L}){return[_,c,w,L].some(Z=>Z!=null)}async function l(_){if(a(_)){const c=await oe.createProceedingDecision(t.documentUnitUuid,_);c.data?s.value=c.data:console.error(c.error)}r()}function r(){o.value=new ee}async function m(_){const c=await oe.linkProceedingDecision(t.documentUnitUuid,_);c.data?(s.value=c.data,g(_)):console.error(c.error),r()}async function u(_){var w;const c=await oe.removeProceedingDecision(t.documentUnitUuid,_.uuid);c.data?(s.value=(w=s.value)==null?void 0:w.filter(L=>L.uuid!==_.uuid),g(_.uuid)):console.error(c.error)}function h(_){return s.value?s.value.some(c=>c.uuid==_.uuid):!1}function g(_){n.value!=null&&(n.value=n.value.map(c=>(c.decision.uuid===_&&(c.isLinked=!c.isLinked),c)))}async function O(){const _=await re.searchByProceedingDecisionInput(o.value);_.data&&(n.value=_.data.map(c=>({decision:c,isLinked:h(c)})))}function I(_){return q("div",{tabindex:_.hasLink?0:-1},[_.hasLink?q(Be,{class:["link-01-bold","underline"],target:"_blank",tabindex:-1,to:{name:"caselaw-documentUnit-:documentNumber-categories",params:{documentNumber:_.documentNumber}}},_.renderDecision):q("span",{class:["link-02-reg"]},_.renderDecision)])}const U=be(I);return F(t,()=>{s.value=t.proceedingDecisions},{immediate:!0}),F(o,()=>{o.value.dateKnown||(o.value.date=void 0)},{immediate:!0,deep:!0}),(_,c)=>(d(),f("div",ks,[ys,v(we,{"as-column":"","data-set":s.value,"fallback-text":"Noch keine vorhergehende Entscheidung hinzugefügt.","summary-component":p(U),title:"Vorgehende Entscheidungen"},{default:C(()=>[s.value?(d(),E(ms,{key:0,decisions:s.value,onRemoveLink:u},null,8,["decisions"])):D("",!0),i("div",xs,[i("div",$s,[v(G,{id:"court",class:"fake-input-group__row__field flex-col",label:"Gericht *"},{default:C(()=>[v(ie,{id:"court",modelValue:o.value.court,"onUpdate:modelValue":c[0]||(c[0]=w=>o.value.court=w),"aria-label":"Gericht Rechtszug","item-service":p(le).getCourts,placeholder:"Gerichtstyp Gerichtsort"},null,8,["modelValue","item-service"])]),_:1}),i("div",Ds,[v(G,{id:"date",class:"w-full",label:"Entscheidungsdatum *"},{default:C(()=>[v(je,{id:"date",modelValue:o.value.date,"onUpdate:modelValue":c[1]||(c[1]=w=>o.value.date=w),"aria-label":"Entscheidungsdatum Rechtszug",disabled:o.value.dateUnknown},null,8,["modelValue","disabled"])]),_:1}),v(G,{id:"dateUnknown",label:"Datum unbekannt","label-position":p(Re).RIGHT},{default:C(()=>[v(He,{id:"dateUnknown",modelValue:o.value.dateUnknown,"onUpdate:modelValue":c[2]||(c[2]=w=>o.value.dateUnknown=w),"aria-label":"Datum Unbekannt"},null,8,["modelValue"])]),_:1},8,["label-position"])])]),i("div",Us,[v(G,{id:"fileNumber",class:"fake-input-group__row__field flex-col",label:"Aktenzeichen *"},{default:C(()=>[v(he,{id:"fileNumber",modelValue:o.value.fileNumber,"onUpdate:modelValue":c[3]||(c[3]=w=>o.value.fileNumber=w),"aria-label":"Aktenzeichen Rechtszug"},null,8,["modelValue"])]),_:1}),v(G,{id:"documentType",class:"fake-input-group__row__field flex-col",label:"Dokumenttyp"},{default:C(()=>[v(ie,{id:"documentType",modelValue:o.value.documentType,"onUpdate:modelValue":c[4]||(c[4]=w=>o.value.documentType=w),"aria-label":"Dokumenttyp Rechtszug","item-service":p(le).getDocumentTypes,placeholder:"Bitte auswählen"},null,8,["modelValue","item-service"])]),_:1})])]),i("div",null,[v(J,{"aria-label":"Nach Entscheidungen suchen","button-type":"secondary",class:"mr-28",label:"Suchen",onClick:O}),v(J,{"aria-label":"Entscheidung manuell hinzufügen","button-type":"tertiary",label:"Manuell Hinzufügen",onClick:c[5]||(c[5]=w=>l(o.value))})]),i("div",Ls,[v(bs,{"search-results":n.value,onLinkDecision:m},null,8,["search-results"])])]),_:1},8,["data-set","summary-component"])]))}});const Cs=V(Ss,[["__scopeId","data-v-2b7159a9"]]),Fs={class:"flex w-full"},Ts=y({__name:"DocumentUnitCategories",props:{documentUnit:null},setup(e){const t=e,s=b(t.documentUnit),n=async k=>{const x=document.createElement("div");x.innerHTML=k[1];const B=x.getElementsByTagName("img").length>0,N=x.getElementsByTagName("table").length>0,Ee=x.innerText.length>0;s.value.texts[k[0]]=Ee||B||N?k[1]:""},o=async()=>{var x;u.value=K.ON_UPDATE;const k=await re.update(s.value);(x=k==null?void 0:k.error)!=null&&x.validationErrors?m.value=k.error.validationErrors:m.value=[],k.data&&(s.value=k.data),I.value=!1,h.value=JSON.stringify(s.value),u.value=k.status},a=Ke(),l=ye(),r=b(navigator.onLine),m=b([]),u=b(K.BEFORE_UPDATE),h=b(JSON.stringify(t.documentUnit)),g=b(""),O=b(),I=b(!1),U=Ye("showDocPanel",l,a.replace,!1);F(U,async()=>{U.value&&g.value.length==0&&await Z()},{immediate:!0});const _=R({get:()=>s.value.coreData,set:k=>{var B,N;let x=!1;((B=s.value.coreData.court)==null?void 0:B.label)!==((N=k.court)==null?void 0:N.label)&&(x=!0),Object.assign(s.value.coreData,k),x&&o()}}),{hash:c}=ke(l);Ge(c);const w=b(!1);function L(){const k=document.getElementById("odoc-panel-element");k&&(k.getBoundingClientRect().top<=0&&U.value===!0?w.value=!0:w.value=!1)}async function Z(){if(!(g.value.length>0)&&t.documentUnit.s3path){const k=await Je.getDocxFileAsHtml(t.documentUnit.s3path);k.error===void 0&&(g.value=k.data)}}const ce=async k=>{if((navigator.userAgent.indexOf("Mac")!=-1?"Mac":"Window")==="Mac"){if(!k.metaKey||k.key!=="s")return;await o(),k.preventDefault()}else{if(!k.ctrlKey||k.key!=="s")return;await o(),k.preventDefault()}},Fe=()=>{O.value=setInterval(async()=>{I.value=JSON.stringify(s.value)!==h.value,r.value&&I.value&&u.value!==K.ON_UPDATE&&await o(),r.value&&!navigator.onLine&&(r.value=!1),!r.value&&navigator.onLine&&(r.value=!0,await o())},1e4)},Te=()=>{clearInterval(O.value)};return de(async()=>{window.addEventListener("scroll",L),window.addEventListener("keydown",ce,!1),Fe(),await Z()}),Ae(()=>{window.removeEventListener("scroll",L),window.removeEventListener("keydown",ce),Te()}),(k,x)=>(d(),E(Ze,{"document-unit":s.value},{default:C(({classes:B})=>[i("div",Fs,[i("div",{class:A(B)},[v(Wn,{id:"coreData",modelValue:p(_),"onUpdate:modelValue":x[0]||(x[0]=N=>Y(_)?_.value=N:null),"update-status":u.value,"validation-errors":m.value.filter(N=>N.field.split(".")[0]==="coreData"),onUpdateDocumentUnit:o},null,8,["modelValue","update-status","validation-errors"]),v(Cs,{id:"proceedingDecisions","document-unit-uuid":s.value.uuid,"proceeding-decisions":e.documentUnit.proceedingDecisions},null,8,["document-unit-uuid","proceeding-decisions"]),v(Yn,{id:"texts",texts:e.documentUnit.texts,"update-status":u.value,onUpdateDocumentUnit:o,onUpdateValue:n},null,8,["texts","update-status"]),v($n,{id:"contentRelatedIndexing","document-unit":t.documentUnit},null,8,["document-unit"])],2),i("div",{class:A(["bg-white border-gray-400 border-l-1 border-solid",{full:p(U)}])},[v(ds,{id:"odoc-panel-element",open:p(U),"onUpdate:open":x[1]||(x[1]=N=>Y(U)?U.value=N:null),class:A(["bg-white",B]),file:g.value,"fixed-panel-position":w.value,"has-file":e.documentUnit.hasFile},null,8,["open","class","file","fixed-panel-position","has-file"])],2)])]),_:1},8,["document-unit"]))}});const Es=V(Ts,[["__scopeId","data-v-cfaab6ff"]]),Os={key:1},Hs=y({__name:"categories",props:{documentNumber:null},async setup(e){let t,s;const n=e,{data:o,error:a}=([t,s]=ge(()=>re.getByDocumentNumber(n.documentNumber)),t=await t,s(),t);return(l,r)=>{var m,u;return p(o)?(d(),E(Es,{key:0,"document-unit":p(o)},null,8,["document-unit"])):(d(),f("div",Os,[i("h2",null,$((m=p(a))==null?void 0:m.title),1),i("p",null,$((u=p(a))==null?void 0:u.description),1)]))}}});export{Hs as default};
